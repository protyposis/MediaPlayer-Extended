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
            // Catch decoder.release() exceptions to avoid breaking the release loop on the first
            // exception and leaking unreleased decoders.
            try {
                decoder.release();
            } catch (Exception e) {
                Log.e(TAG, "release failed", e);
            }
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
        int eosCount = 0;
        for (MediaCodecDecoder decoder : mDecoders) {
            if(decoder.isOutputEos()) {
                eosCount++;
            }
        }
        return eosCount == mDecoders.size();
    }

    public long getCachedDuration() {
        // Init with the largest possible value...
        long minCachedDuration = Long.MAX_VALUE;

        // ...then decrease to the lowest duration.
        // We always return the lowest value, because if only one decoder has to refill its buffer,
        // all others have to wait. If one decoder returns -1, this function returns -1 too (which
        // makes sense because we cannot calculate a meaningful cache duration in this case).
        for (MediaCodecDecoder decoder : mDecoders) {
            long cachedDuration = decoder.getCachedDuration();
            minCachedDuration = Math.min(cachedDuration, minCachedDuration);
        }

        if(minCachedDuration == Long.MAX_VALUE) {
            // There were no decoders that updated this value, which means we don't have information
            // on a cached duration, so we return -1 to signal that the information is not available.
            return -1;
        }

        return minCachedDuration;
    }

    /**
     * Returns true only if all decoders have reached the end of stream.
     */
    public boolean hasCacheReachedEndOfStream() {
        for (MediaCodecDecoder decoder : mDecoders) {
            if(!decoder.hasCacheReachedEndOfStream()) {
                return false;
            }
        }
        return true;
    }
}
