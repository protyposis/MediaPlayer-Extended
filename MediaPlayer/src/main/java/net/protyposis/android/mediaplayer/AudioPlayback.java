/*
 * Copyright 2014 Mario Guggenberger <mg@protyposis.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.protyposis.android.mediaplayer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Wrapper of an AudioTrack for easier management in the playback thread.
 *
 * Created by maguggen on 23.09.2014.
 */
class AudioPlayback {

    private static final String TAG = AudioPlayback.class.getSimpleName();

    public static long PTS_NOT_SET = Long.MIN_VALUE;

    private MediaFormat mAudioFormat;
    private AudioTrack mAudioTrack;
    private byte[] mTransferBuffer;
    private int mFrameChunkSize;
    private int mFrameSize;
    private int mSampleRate;
    private BufferQueue mBufferQueue;
    private int mPlaybackBufferSize;
    private AudioThread mAudioThread;
    private long mLastPresentationTimeUs;
    private int mAudioSessionId;
    private int mAudioStreamType;
    private float mVolumeLeft = 1, mVolumeRight = 1;

    /**
     * Keeps track of the PTS of the moment when playback has started.
     * It is required to calculate the current PTS because the playback head
     * is reset to zero when playback is paused.
     */
    private long mPresentationTimeOffsetUs;

    /**
     * Hold the previous playback head position time for comparison with the current playback
     * head position time to detect a position wrap/overflow.
     */
    private long mLastPlaybackHeadPositionUs;

    public AudioPlayback() {
        mFrameChunkSize = 4096 * 2; // arbitrary default chunk size
        mBufferQueue = new BufferQueue();
        mAudioSessionId = 0; // AudioSystem.AUDIO_SESSION_ALLOCATE;
        mAudioStreamType = AudioManager.STREAM_MUSIC;
    }

    /**
     * Initializes or reinitializes the audio track with the supplied format for playback
     * while keeping the playstate. Keeps the current configuration and skips reinitialization
     * if the new format is the same as the current format.
     */
    public void init(MediaFormat format) {
        Log.d(TAG, "init");

        boolean playing = false;

        if(isInitialized()) {
            if(!checkIfReinitializationRequired(format)) {
                // Set new format that equals the old one (in case we compare references somewhere)
                mAudioFormat = format;
                return;
            }

            playing = isPlaying();
            pause();
            stopAndRelease(false);
        } else {
            // deferred creation of the audio thread until its first use
            mAudioThread = new AudioThread();
            mAudioThread.setPaused(true);
            mAudioThread.start();
        }

        mAudioFormat = format;

        int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        int bytesPerSample = 2;
        mFrameSize = bytesPerSample * channelCount;
        mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);

        int channelConfig = AudioFormat.CHANNEL_OUT_DEFAULT;
        switch(channelCount) {
            case 1:
                channelConfig = AudioFormat.CHANNEL_OUT_MONO;
                break;
            case 2:
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
                break;
            case 4:
                channelConfig = AudioFormat.CHANNEL_OUT_QUAD;
                break;
            case 6:
                channelConfig = AudioFormat.CHANNEL_OUT_5POINT1;
                break;
            case 8:
                channelConfig = AudioFormat.CHANNEL_OUT_7POINT1;
        }

        mPlaybackBufferSize = mFrameChunkSize * channelCount;

        mAudioTrack = new AudioTrack(
                mAudioStreamType,
                mSampleRate,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT,
                mPlaybackBufferSize, // at least twice the size to enable double buffering (according to docs)
                AudioTrack.MODE_STREAM, mAudioSessionId);

        if(mAudioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            stopAndRelease();
            throw new IllegalStateException("audio track init failed");
        }

        mAudioSessionId = mAudioTrack.getAudioSessionId();
        mAudioStreamType = mAudioTrack.getStreamType();
        setStereoVolume(mVolumeLeft, mVolumeRight);
        mPresentationTimeOffsetUs = PTS_NOT_SET;

        if(playing) {
            play();
        }
    }

    private boolean checkIfReinitializationRequired(MediaFormat newFormat) {
        return mAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) != newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                || mAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) != newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                || !mAudioFormat.getString(MediaFormat.KEY_MIME).equals(newFormat.getString(MediaFormat.KEY_MIME));
    }

    /**
     * Can be used to set an audio session ID before calling {@link #init(android.media.MediaFormat)}.
     */
    public void setAudioSessionId(int sessionId) {
        if(isInitialized()) {
            throw new IllegalStateException("cannot set session id on an initialized audio track");
        }
        mAudioSessionId = sessionId;
    }

    public int getAudioSessionId() {
        return mAudioSessionId;
    }

    public void setAudioStreamType(int streamType) {
        mAudioStreamType = streamType;
    }

    public int getAudioStreamType() {
        return mAudioStreamType;
    }

    public boolean isInitialized() {
        return mAudioTrack != null && mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED;
    }

    public void play() {
        //Log.d(TAG, "play");
        if(isInitialized()) {
            mAudioTrack.play();
            mAudioThread.setPaused(false);
        } else {
            throw new IllegalStateException();
        }
    }

    public void pause(boolean flush) {
        //Log.d(TAG, "pause(" + flush + ")");
        if(isInitialized()) {
            mAudioThread.setPaused(true);
            mAudioTrack.pause();

            if(flush) {
                flush();
            }
        } else {
            throw new IllegalStateException();
        }
    }

    public void pause() {
        pause(true);
    }

    public void flush() {
        if(isInitialized()) {
            boolean playing = isPlaying();
            if(playing) {
                mAudioTrack.pause();
            }
            mAudioTrack.flush();
            mBufferQueue.flush();

            // Reset offset so it gets updated with the current PTS when playback continues
            mPresentationTimeOffsetUs = PTS_NOT_SET;

            if(playing) {
                mAudioTrack.play();
            }
        } else {
            throw new IllegalStateException();
        }
    }

    public void write(ByteBuffer audioData, long presentationTimeUs) {
        int sizeInBytes = audioData.remaining();

        // TODO find a way to determine the audio decoder max output frame size at configuration time
        if(mFrameChunkSize < sizeInBytes) {
            Log.d(TAG, "incoming frame chunk size increased to " + sizeInBytes);
            mFrameChunkSize = sizeInBytes;
            // re-init the audio track to accommodate buffer to new chunk size
            init(mAudioFormat);
        }

        // Special handling of the first written audio buffer after a flush (pause with flush)
        if(mPresentationTimeOffsetUs == PTS_NOT_SET) {
            // Initialize with the PTS of the first audio buffer (which isn't necessarily zero)
            mPresentationTimeOffsetUs = presentationTimeUs;
            mLastPlaybackHeadPositionUs = 0;

            /** Handle playback head reset bug
             *
             * affected: Galaxy S2 API 16
             * not affected: Nexus 4 API 22
             *
             * Sometimes the playback head does not really reset to zero in a pause. During the
             * pause, it correctly returns zero (0), but when playback continues it sometimes
             * continues from the previous playback head position instead of starting from zero.
             * Since this does not always happen, this looks to be a bug in the Android framework.
             *
             * TODO find out if this is a reported bug
             *
             * To work around this issue, we subtract the playback head position time from the PTS
             * offset to adjust the base time by the playback head time. This leads to the
             * {@link #getCurrentPresentationTimeUs} method returning a correct value.
             */
            long playbackHeadPositionUs = getPlaybackheadPositionUs();
            if(playbackHeadPositionUs > 0) {
                mPresentationTimeOffsetUs -= playbackHeadPositionUs;
                Log.d(TAG, "playback head not reset");
            }
        }

        mBufferQueue.put(audioData, presentationTimeUs);
//        Log.d(TAG, "buffer queue size " + mBufferQueue.bufferQueue.size()
//                + " data " + mBufferQueue.mQueuedDataSize
//                + " time " + getQueueBufferTimeUs());
        mAudioThread.notifyOfNewBufferInQueue();
    }

    private void stopAndRelease(boolean killThread) {
        if(killThread && mAudioThread != null) {
            mAudioThread.interrupt();
        }

        if(mAudioTrack != null) {
            if(isInitialized()) {
                mAudioTrack.stop();
            }
            mAudioTrack.release();
        }
        mAudioTrack = null;
    }

    public void stopAndRelease() {
        stopAndRelease(true);
    }

    /**
     * Returns the length of the queued audio, that does not fit into the playback buffer yet.
     * @return the length of the queued audio in microsecs
     */
    public long getQueueBufferTimeUs() {
        return (long)((double)(mBufferQueue.mQueuedDataSize / mFrameSize)
                / mSampleRate * 1000000d);
    }

    /**
     * Returns the length of the playback buffer, without posidering the current playback position
     * inside the buffer (the remaining audio data that is waiting for playback can be less than
     * the buffer length).
     * @return the length of the playback buffer in microsecs
     */
    public long getPlaybackBufferTimeUs() {
        return (long)((double)(mPlaybackBufferSize / mFrameSize) / mSampleRate * 1000000d);
    }

    private long getPlaybackheadPositionUs() {
        // The playback head position is encoded as a uint in an int
        long playbackHeadPosition = 0xFFFFFFFFL & mAudioTrack.getPlaybackHeadPosition();
        // Convert frames to time
        return (long)((double)playbackHeadPosition / mSampleRate * 1000000);
    }

    /**
     * Returns the current PTS of the playback head or PTS_NOT_SET if the PTS cannot be reliably
     * calculated yet.
     * For this method to return a PTS, audio samples need to be written before ({@link #write(ByteBuffer, long)}.
     * @return the PTS at the playback head or PTS_NOT_SET if unknown
     */
    public long getCurrentPresentationTimeUs() {
        // Return the PTS_NOT_SET flag when the PTS has not been initialized yet. At the start of
        // media playback, returning the playback head alone is reliable, but later on (e.g. after a
        // seek), a missing PTS offset leads to totally wrong values.
        if(mPresentationTimeOffsetUs == PTS_NOT_SET) {
            return PTS_NOT_SET;
        }

        long playbackHeadPositionUs = getPlaybackheadPositionUs();

        // Handle playback head wrapping
        if(playbackHeadPositionUs < mLastPlaybackHeadPositionUs) {
            // playback head position has wrapped around it's 32bit uint value
            Log.d(TAG, "playback head has wrapped");
            // Add the full runtime to the PTS offset to advance it one playback head iteration
            mPresentationTimeOffsetUs += (long)((double)0xFFFFFFFF / mSampleRate * 1000000);
        }
        mLastPlaybackHeadPositionUs = playbackHeadPositionUs;

        // Return the playback head time, offset by the start offset PTS
        return mPresentationTimeOffsetUs + playbackHeadPositionUs;
    }

    public long getLastPresentationTimeUs() {
        return mLastPresentationTimeUs;
    }

    public void setPlaybackSpeed(float speed) {
        if(isInitialized()) {
           mAudioTrack.setPlaybackRate((int)(mSampleRate * speed));
        } else {
            throw new IllegalStateException();
        }
    }

    public boolean isPlaying() {
        return mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
    }

    private void writeToPlaybackBuffer(ByteBuffer audioData, long presentationTimeUs) {
        int size = audioData.remaining();
        if(mTransferBuffer == null || mTransferBuffer.length < size) {
            mTransferBuffer = new byte[size];
        }
        audioData.get(mTransferBuffer, 0, size);

        //Log.d(TAG, "audio write / chunk count " + mPlaybackBufferChunkCount);
        mLastPresentationTimeUs = presentationTimeUs;
        mAudioTrack.write(mTransferBuffer, 0, size);
    }

    /**
     * @see android.media.AudioTrack#setStereoVolume(float, float)
     * @deprecated deprecated in API21, prefer use of {@link #setVolume(float)}
     */
    public void setStereoVolume(float leftGain, float rightGain) {
        mVolumeLeft = leftGain;
        mVolumeRight = rightGain;

        if(mAudioTrack != null) {
            mAudioTrack.setStereoVolume(leftGain, rightGain);
        }
    }

    /**
     * @see android.media.AudioTrack#setVolume(float)
     */
    public void setVolume(float gain) {
        //@TargetApi(Build.VERSION_CODES.LOLLIPOP)
        //mAudioTrack.setVolume(gain);

        setStereoVolume(gain, gain);
    }

    /*
     * This thread reads buffers from the queue and supplies them to the playback buffer. If the
     * queue is empty, it waits until a buffer item becomes available. If the playback buffer is
     * full, it blocks until it empties because of the AudioTrack#write blocking behaviour, and
     * since this is a separate audio thread, it does not block the video playback thread.
     * The thread is necessary because the AudioTrack#setPositionNotificationPeriod + listener
     * combination does only seem top work reliably if the written frame chunk sizes are constant
     * and the notification period is set to exactly this chunk size, which is impossible when
     * dealing with variable chunk sizes. Workarounds would be to set the notification period to the
     * least common multiple and split the written chunk also in pieces of this size (not sure if
     * very small notifications work though), or to add a transformation layer in the queue that
     * redistributes the incoming chunks of variable size into chunks of constant size; both
     * solutions would be more complex than this thread and also add noticeable overhead (many
     * method calls in the first workaround, many data copy operations in the second).
     */
    private class AudioThread extends Thread {

        private final Object SYNC = new Object();
        private boolean mPaused;

        AudioThread() {
            super(TAG);
            mPaused = true;
        }

        void setPaused(boolean paused) {
            mPaused = paused;
            synchronized (this) {
                this.notify();
            }
        }

        public void notifyOfNewBufferInQueue() {
            synchronized (SYNC) {
                SYNC.notify();
            }
        }

        @Override
        public void run() {
            while(!isInterrupted()) {
                try {
                    synchronized(this) {
                        while(mPaused) {
                            wait();
                        }
                    }

                    BufferQueue.Item bufferItem = null;
                    synchronized (SYNC) {
                        while ((bufferItem = mBufferQueue.take()) == null) {
                            SYNC.wait();
                        }
                    }

                    writeToPlaybackBuffer(bufferItem.buffer, bufferItem.presentationTimeUs);
                    mBufferQueue.put(bufferItem);
                } catch (InterruptedException e) {
                    interrupt();
                }
            }
        }

    }

    /**
     * Intermediate buffer queue for audio chunks. When an audio chunk is decoded, it is put into
     * this queue until the audio track periodic notification event gets fired, telling that a certain
     * amount of the audio playback buffer has been consumed, which then enqueues another chunk to
     * the playback output buffer.
     */
    private static class BufferQueue {

        private static class Item {
            ByteBuffer buffer;
            long presentationTimeUs;

            Item(int size) {
                buffer = ByteBuffer.allocate(size);
            }
        }

        private int bufferSize;
        private Queue<Item> bufferQueue;
        private List<Item> emptyBuffers;
        private int mQueuedDataSize;

        BufferQueue() {
            bufferQueue = new LinkedList<Item>();
            emptyBuffers = new ArrayList<Item>();
        }

        synchronized void put(ByteBuffer data, long presentationTimeUs) {
            //Log.d(TAG, "put");
            if(data.remaining() > bufferSize) {
                /* Buffer size has increased, invalidate all empty buffers since they can not be
                 * reused any more. */
                emptyBuffers.clear();
                bufferSize = data.remaining();
            }

            Item item;
            if(!emptyBuffers.isEmpty()) {
                item = emptyBuffers.remove(0);
            } else {
                item = new Item(data.remaining());
            }

            item.buffer.limit(data.remaining());
            item.buffer.mark();
            item.buffer.put(data);
            item.buffer.reset();
            item.presentationTimeUs = presentationTimeUs;

            bufferQueue.add(item);
            mQueuedDataSize += item.buffer.remaining();
        }

        /**
         * Takes a buffer item out of the queue to read the data. Returns NULL if there is no
         * buffer ready.
         */
        synchronized Item take() {
            //Log.d(TAG, "take");
            Item item = bufferQueue.poll();
            if(item != null) {
                mQueuedDataSize -= item.buffer.remaining();
            }
            return item;
        }

        /**
         * Returns a buffer to the queue for reuse.
         */
        synchronized void put(Item returnItem) {
            if(returnItem.buffer.capacity() != bufferSize) {
                /* The buffer size has changed and the returned buffer is not valid any more and
                 * can be discarded. */
                return;
            }

            returnItem.buffer.rewind();
            emptyBuffers.add(returnItem);
        }

        /**
         * Removes all remaining buffers from the queue and returns them to the empty-item store.
         */
        synchronized void flush() {
            Item item;
            while((item = bufferQueue.poll()) != null) {
                put(item);
            }
            mQueuedDataSize = 0;
        }
    }
}
