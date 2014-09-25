/*
 * Copyright (c) 2014 Mario Guggenberger <mario.guggenberger@aau.at>
 *
 * This file is part of ITEC MediaPlayer.
 *
 * ITEC MediaPlayer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ITEC MediaPlayer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ITEC MediaPlayer.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.aau.itec.android.mediaplayer;

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

    private MediaFormat mAudioFormat;
    private AudioTrack mAudioTrack;
    private byte[] mTransferBuffer;
    private int mFrameChunkSize;
    private int mFrameSize;
    private int mSampleRate;
    private BufferQueue mBufferQueue;
    private int mPlaybackBufferSizeFactor;
    private AudioThread mAudioThread;
    private long mLastPresentationTimeUs;
    private int mAudioSessionId;

    public AudioPlayback() {
        mPlaybackBufferSizeFactor = 4; // works for now; low dropouts but not too large
        mFrameChunkSize = 4096; // arbitrary default chunk size
        mBufferQueue = new BufferQueue();
    }

    /**
     * Initializes or reinitializes the audio track with the supplied format for playback
     * while keeping the playstate.
     */
    public void init(MediaFormat format) {
        Log.d(TAG, "init");
        mAudioFormat = format;

        boolean playing = false;

        if(isInitialized()) {
            playing = isPlaying();
            pause();
            stopAndRelease(false);
        } else {
            // deferred creation of the audio thread until its first use
            mAudioThread = new AudioThread();
            mAudioThread.setPaused(true);
            mAudioThread.start();
        }

        int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        int bytesPerSample = 2;
        mFrameSize = bytesPerSample * channelCount;
        mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);

        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                mSampleRate,
                channelCount == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mFrameChunkSize * mPlaybackBufferSizeFactor, // at least twice the size to enable double buffering (according to docs)
                AudioTrack.MODE_STREAM, mAudioSessionId);
        mAudioSessionId = mAudioTrack.getAudioSessionId();

        if(playing) {
            play();
        }
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

    public boolean isInitialized() {
        return mAudioTrack != null;
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

        mBufferQueue.put(audioData, presentationTimeUs);
//        Log.d(TAG, "buffer queue size " + mBufferQueue.bufferQueue.size()
//                + " data " + mBufferQueue.mQueuedDataSize
//                + " time " + getBufferTimeUs());
        mAudioThread.notifyOfNewBufferInQueue();
    }

    private void stopAndRelease(boolean killThread) {
        if(isInitialized()) {
            if(killThread) mAudioThread.interrupt();
            mAudioTrack.stop();
            mAudioTrack.release();
        }
        mAudioTrack = null;
    }

    public void stopAndRelease() {
        stopAndRelease(true);
    }

    public long getBufferTimeUs() {
        return (long)((double)(mBufferQueue.mQueuedDataSize / mFrameSize)
                / mSampleRate * 1000000d);
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

    private boolean isPlaying() {
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
