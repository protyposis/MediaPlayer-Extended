/*
 * Copyright (c) 2016 Mario Guggenberger <mg@protyposis.net>
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

package net.protyposis.android.mediaplayer;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.io.IOException;

/**
 * Created by Mario on 20.04.2016.
 */
class MediaCodecAudioDecoder extends MediaCodecDecoder {

    private AudioPlayback mAudioPlayback;

    public MediaCodecAudioDecoder(MediaExtractor extractor, boolean passive, int trackIndex,
                                  OnDecoderEventListener listener, AudioPlayback audioPlayback)
            throws IOException {
        super(extractor, passive, trackIndex, listener);
        mAudioPlayback = audioPlayback;
        reinitCodec();
    }

    @Override
    protected void configureCodec(MediaCodec codec, MediaFormat format) {
        super.configureCodec(codec, format);
        mAudioPlayback.init(format);
    }

    @Override
    protected boolean shouldDecodeAnotherFrame() {
        // If this is an active audio track, decode and buffer only as much as this arbitrarily
        // chosen threshold time to avoid filling up the memory with buffered audio data and
        // requesting too much data from the network too fast (e.g. DASH segments).
        if(!isPassive()) {
            return mAudioPlayback.getQueueBufferTimeUs() < 200000;
        }
        else {
            return super.shouldDecodeAnotherFrame();
        }
    }

    @Override
    public void renderFrame(FrameInfo frameInfo, long offsetUs) {
        mAudioPlayback.write(frameInfo.data, frameInfo.presentationTimeUs);
        releaseFrame(frameInfo);
    }
}
