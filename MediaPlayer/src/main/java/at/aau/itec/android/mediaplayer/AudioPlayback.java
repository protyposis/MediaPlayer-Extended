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
class AudioPlayback implements AudioTrack.OnPlaybackPositionUpdateListener {

    private static final String TAG = AudioPlayback.class.getSimpleName();

    private MediaFormat mAudioFormat;
    private AudioTrack mAudioTrack;
    private byte[] mTransferBuffer;
    private int mFrameChunkSize;
    private int mFrameSize;
    private int mSampleRate;
    private BufferQueue mBufferQueue;
    private int mPlaybackBufferSizeFactor;
    private int mPlaybackBufferChunkCount;

    public AudioPlayback() {
        mPlaybackBufferSizeFactor = 4; // works for now; low dropouts but not too large
        mFrameChunkSize = 8192; // arbitrary default chunk size
        mBufferQueue = new BufferQueue();
    }

    /**
     * Initializes or reinitializes the audio track with the supplied format for playback
     * while keeping the playstate.
     */
    public void init(MediaFormat format) {
        Log.w(TAG, "init");
        mAudioFormat = format;

        boolean playing = false;

        if(isInitialized()) {
            playing = isPlaying();
            stopAndRelease();
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
                AudioTrack.MODE_STREAM);

        mAudioTrack.setPlaybackPositionUpdateListener(this);
        mAudioTrack.setPositionNotificationPeriod(mFrameChunkSize / mFrameSize);
        mPlaybackBufferChunkCount = 0;

        if(playing) {
            play();
        }
    }

    public boolean isInitialized() {
        return mAudioTrack != null;
    }

    public void play() {
        if(isInitialized()) {
            mAudioTrack.setPlaybackPositionUpdateListener(this);
            mAudioTrack.setPositionNotificationPeriod(mFrameChunkSize / mFrameSize);
            mAudioTrack.play();
        } else {
            throw new IllegalStateException();
        }
    }

    public void pause(boolean flush) {
        if(isInitialized()) {
            mAudioTrack.pause();
            if(flush) {
                mAudioTrack.flush();
                mBufferQueue.flush();
                mPlaybackBufferChunkCount = 0;
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
            if(playing) {
                mAudioTrack.play();
            }
        } else {
            throw new IllegalStateException();
        }
    }

    public void write(ByteBuffer audioData) {
        int sizeInBytes = audioData.remaining();

        // TODO find a way to determine the audio decoder output frame size at configuration time
        if(mFrameChunkSize != sizeInBytes) {
            Log.d(TAG, "incoming frame chunk size changed to " + sizeInBytes);
            mFrameChunkSize = sizeInBytes;
            // re-init the audio track to accommodate buffer to new chunk size
            init(mAudioFormat);
        }

        if(mPlaybackBufferChunkCount < mPlaybackBufferSizeFactor) {
            writeToPlaybackBuffer(audioData);
        } else {
            mBufferQueue.put(audioData);
//            Log.d(TAG, "buffer queue size " + mBufferQueue.bufferQueue.size()
//                    + " data " + mBufferQueue.mQueuedDataSize
//                    + " time " + bufferTimeUs());
        }
    }

    public void stopAndRelease() {
        if(isInitialized()) {
            mAudioTrack.stop();
            mAudioTrack.release();
        }
        mAudioTrack = null;
    }

    public long bufferTimeUs() {
        return (long)((double)(mBufferQueue.mQueuedDataSize / mFrameSize)
                / mSampleRate * 1000000d);
    }

    private boolean isPlaying() {
        return mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
    }

    private void writeToPlaybackBuffer(ByteBuffer audioData) {
        int size = audioData.remaining();
        if(mTransferBuffer == null || mTransferBuffer.length < size) {
            mTransferBuffer = new byte[size];
        }
        audioData.get(mTransferBuffer, 0, size);

        //Log.d(TAG, "audio write / chunk count " + mPlaybackBufferChunkCount);
        mAudioTrack.write(mTransferBuffer, 0, size);
        mPlaybackBufferChunkCount++;
    }

    @Override
    public void onMarkerReached(AudioTrack track) {
        // nop
    }

    @Override
    public void onPeriodicNotification(AudioTrack track) {
        //Log.d(TAG, "onPeriodicNotification");
        mPlaybackBufferChunkCount--;

        if(!isInitialized()) {
            return; // avoid NPE on shutdown
        }

        // queue next frame chunk to the audio output buffer if available
        BufferQueue.Item bufferItem = mBufferQueue.take();

        if(bufferItem == null) {
            Log.w(TAG, " audio stream drained");
            return;
        }

        writeToPlaybackBuffer(bufferItem.buffer);
        mBufferQueue.put(bufferItem);
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

        synchronized void put(ByteBuffer data) {
            if(data.remaining() != bufferSize) {
                /* Buffer size has changed, invalidate all empty buffers since they can not be
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

            item.buffer.mark();
            item.buffer.put(data);
            item.buffer.reset();

            bufferQueue.add(item);
            mQueuedDataSize += item.buffer.remaining();
        }

        /**
         * Takes a buffer item out of the queue to read the data. Returns NULL if there is no
         * buffer ready.
         */
        synchronized Item take() {
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
