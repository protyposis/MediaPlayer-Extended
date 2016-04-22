/*
 * Copyright (c) 2015 Mario Guggenberger <mg@protyposis.net>
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

import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mario on 13.09.2015.
 */
class Decoder {

    private static final String TAG = Decoder.class.getSimpleName();

    private List<MediaCodecDecoder> mDecoders;
    private MediaCodecVideoDecoder mVideoDecoder;
    private MediaCodecAudioDecoder mAudioDecoder;

    public Decoder(MediaExtractor videoExtractor, int videoTrackIndex, Surface videoSurface, boolean renderModeApi21,
                   MediaExtractor audioExtractor, int audioTrackIndex, AudioPlayback audioPlayback,
                   MediaCodecDecoder.OnDecoderEventListener listener)
            throws IllegalStateException, IOException
    {
        mDecoders = new ArrayList<>();

        if(videoTrackIndex != MediaCodecDecoder.INDEX_NONE) {
            mVideoDecoder = new MediaCodecVideoDecoder(videoExtractor, false, videoTrackIndex, listener, videoSurface, renderModeApi21);
            mDecoders.add(mVideoDecoder);
        }

        if(audioTrackIndex != MediaCodecDecoder.INDEX_NONE) {
            boolean passive = (audioExtractor == videoExtractor || audioExtractor == null);
            mAudioDecoder = new MediaCodecAudioDecoder(audioExtractor != null ? audioExtractor : videoExtractor, passive, audioTrackIndex, listener, audioPlayback);
            mDecoders.add(mAudioDecoder);
        }
    }

    public MediaCodecVideoDecoder getVideoDecoder() {
        return mVideoDecoder;
    }

    /**
     * Runs the audio/video decoder loop, optionally until a new frame is available.
     * The returned rameInfo object keeps metadata of the decoded frame. To render the frame
     * to the screen and/or dismiss its data, call {@link MediaCodecVideoDecoder#releaseFrame(MediaCodecDecoder.FrameInfo, boolean)}
     * or {@link MediaCodecVideoDecoder#releaseFrame(MediaCodecDecoder.FrameInfo, long)}.
     *
     * @param videoOnly skip audio frames
     * @param force force decoding in a loop until a frame becomes available or the EOS is reached
     * @return a VideoFrameInfo object holding metadata of a decoded video frame or NULL if no frame has been decoded
     */
    public MediaCodecDecoder.FrameInfo decodeFrame(boolean videoOnly, boolean force) {
        //Log.d(TAG, "decodeFrame");
        boolean outputEos = false;

        while(!outputEos) {
            int outputEosCount = 0;
            MediaCodecDecoder.FrameInfo fi;
            MediaCodecDecoder.FrameInfo vfi = null;

            for (MediaCodecDecoder decoder : mDecoders) {
                while((fi = decoder.dequeueDecodedFrame()) != null) {

                    if(decoder == mVideoDecoder) {
                        vfi = fi;
                        break;
                    } else {
                        decoder.renderFrame(fi, 0);
                    }
                }

                while (decoder.queueSampleToCodec(false)) {}

                if(decoder.isOutputEos()) {
                    outputEosCount++;
                }
            }

            if(vfi != null) {
                // If a video frame has been decoded, return it
                return vfi;
            }

            if(!force) {
                // If we have not decoded a video frame and we're not forcing decoding until a frame
                // becomes available, return null.
                return null;
            }

            outputEos = (outputEosCount == mDecoders.size());
        }

        Log.d(TAG, "EOS NULL");
        return null; // EOS already reached, no video frame left to return
    }

    /**
     * Releases all decoders. This must be called to free decoder resources when this object is no longer in use.
     */
    public void release() {
        for (MediaCodecDecoder decoder : mDecoders) {
            decoder.release();
        }
        mDecoders.clear();
    }

    public void seekTo(MediaPlayer.SeekMode seekMode, long seekTargetTimeUs) throws IOException {
        for (MediaCodecDecoder decoder : mDecoders) {
            decoder.seekTo(seekMode, seekTargetTimeUs);
        }
    }

    public void renderFrames() {
        for (MediaCodecDecoder decoder : mDecoders) {
            decoder.renderFrame();
        }
    }

    public void dismissFrames() {
        for (MediaCodecDecoder decoder : mDecoders) {
            decoder.dismissFrame();
        }
    }

    public long getCurrentDecodingPTS() {
        long minPTS = Long.MAX_VALUE;
        for (MediaCodecDecoder decoder : mDecoders) {
            long pts = decoder.getCurrentDecodingPTS();
            if(pts != MediaCodecDecoder.PTS_NONE && minPTS > pts) {
                minPTS = pts;
            }
        }
        return minPTS;
    }

    public boolean isEOS() {
        //return getCurrentDecodingPTS() == MediaCodecDecoder.PTS_EOS;
        for (MediaCodecDecoder decoder : mDecoders) {
            if(decoder.isOutputEos()) {
                return true;
            }
        }
        return false;
    }

    public long getCachedDuration() {
        long minCachedDuration = -1;
        for (MediaCodecDecoder decoder : mDecoders) {
            long cachedDuration = decoder.getCachedDuration();
            if(cachedDuration != -1 && minCachedDuration > cachedDuration) {
                minCachedDuration = cachedDuration;
            }
        }
        return minCachedDuration;
    }
}
