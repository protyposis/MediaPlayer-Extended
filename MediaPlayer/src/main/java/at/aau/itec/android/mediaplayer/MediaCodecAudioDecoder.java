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

package at.aau.itec.android.mediaplayer;

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
        reconfigureCodec();
    }

    @Override
    protected void configureCodec(MediaCodec codec, MediaFormat format) {
        super.configureCodec(codec, format);
        mAudioPlayback.init(format);
    }

    @Override
    public void releaseFrame(FrameInfo frameInfo) {
        mAudioPlayback.write(frameInfo.data, frameInfo.presentationTimeUs);
        getCodec().releaseOutputBuffer(frameInfo.buffer, false);
        releaseFrameInfo(frameInfo);
    }
}
