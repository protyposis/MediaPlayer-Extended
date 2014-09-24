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

    public AudioPlayback() {
        mFrameChunkSize = 8192; // arbitrary default chunk size
    }

    /**
     * Initializes or reinitializes the audio track with the supplied format for playback
     * while keeping the playstate.
     */
    public void init(MediaFormat format) {
        mAudioFormat = format;

        boolean playing = false;

        if(isInitialized()) {
            playing = isPlaying();
            stopAndRelease();
        }
        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mFrameChunkSize * 2, // twice the size to enable double buffering (according to docs)
                AudioTrack.MODE_STREAM);

        if(playing) {
            play();
        }
    }

    public boolean isInitialized() {
        return mAudioTrack != null;
    }

    public void play() {
        if(isInitialized()) {
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

    public int write(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        // TODO find a way to determine the audio decoder output frame size at configuration time
        if(mFrameChunkSize != sizeInBytes) {
            Log.d(TAG, "incoming frame chunk size changed to " + sizeInBytes);
            mFrameChunkSize = sizeInBytes;
            // re-init the audio track to accommodate buffer to new chunk size
            init(mAudioFormat);
        }

        if(isInitialized()) {
            return mAudioTrack.write(audioData, offsetInBytes, sizeInBytes);
        } else {
            throw new IllegalStateException();
        }
    }

    public int write(ByteBuffer audioData) {
        int size = audioData.remaining();
        if(mTransferBuffer == null || mTransferBuffer.length < size) {
            mTransferBuffer = new byte[size];
        }
        audioData.get(mTransferBuffer, 0, size);
        return write(mTransferBuffer, 0, size);
    }

    public void stopAndRelease() {
        if(isInitialized()) {
            mAudioTrack.stop();
            mAudioTrack.release();
        }
        mAudioTrack = null;
    }

    private boolean isPlaying() {
        return mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
    }
}
