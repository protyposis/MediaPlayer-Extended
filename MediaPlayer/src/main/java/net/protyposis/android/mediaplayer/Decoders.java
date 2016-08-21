/*
 * Copyright 2015 Mario Guggenberger <mg@protyposis.net>
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

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mario on 13.09.2015.
 */
class Decoders {

    private static final String TAG = Decoders.class.getSimpleName();

    private List<MediaCodecDecoder> mDecoders;
    private MediaCodecVideoDecoder mVideoDecoder;
    private MediaCodecAudioDecoder mAudioDecoder;

    public Decoders() {
        mDecoders = new ArrayList<>();
    }

    public void addDecoder(MediaCodecDecoder decoder) {
        mDecoders.add(decoder);

        if (decoder instanceof MediaCodecVideoDecoder) {
            mVideoDecoder = (MediaCodecVideoDecoder) decoder;
        } else if (decoder instanceof MediaCodecAudioDecoder) {
            mAudioDecoder = (MediaCodecAudioDecoder) decoder;
        }
    }

    public List<MediaCodecDecoder> getDecoders() {
        return mDecoders;
    }

    public MediaCodecVideoDecoder getVideoDecoder() {
        return mVideoDecoder;
    }

    public MediaCodecAudioDecoder getAudioDecoder() {
        return mAudioDecoder;
    }

    /**
     * Runs the audio/video decoder loop, optionally until a new frame is available.
     * The returned frameInfo object keeps metadata of the decoded frame. To render the frame
     * to the screen and/or dismiss its data, call {@link MediaCodecVideoDecoder#releaseFrame(MediaCodecDecoder.FrameInfo, boolean)}
     * or {@link MediaCodecVideoDecoder#releaseFrame(MediaCodecDecoder.FrameInfo, long)}.
     *
     * @param force force decoding in a loop until a frame becomes available or the EOS is reached
     * @return a VideoFrameInfo object holding metadata of a decoded video frame or NULL if no frame has been decoded
     */
    public MediaCodecDecoder.FrameInfo decodeFrame(boolean force) {
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
