/*
 * Copyright (c) 2014 Mario Guggenberger <mg@itec.aau.at>
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

import java.io.File;
import java.io.IOException;

/**
 * Created by Mario on 25.01.2015.
 */
public class FileSource implements MediaSource {

    private File mFile;
    private File mAudioFile;

    /**
     * Creates a media source from a local file. The file can be either video-only, or multiplexed
     * audio/video.
     * @param file the av source file
     */
    public FileSource(File file) {
        mFile = file;
    }

    /**
     * Creates a media source from separate local video and audio files.
     * @param videoFile the video source file
     * @param audioFile the audio source file
     */
    public FileSource(File videoFile, File audioFile) {
        mFile = videoFile;
        mAudioFile = audioFile;
    }

    @Override
    public MediaExtractor getVideoExtractor() throws IOException {
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(mFile.getAbsolutePath());
        return mediaExtractor;
    }

    @Override
    public MediaExtractor getAudioExtractor() throws IOException {
        if(mAudioFile != null) {
            // In case of a separate audio file, return an audio extractor
            MediaExtractor mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(mAudioFile.getAbsolutePath());
            return mediaExtractor;
        }
        // We do not need a separate audio extractor when only a single (multiplexed) file
        // is passed into this class.
        return null;
    }
}
