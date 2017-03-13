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

    public File getFile() {
        return mFile;
    }

    public File getAudioFile() {
        return mAudioFile;
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
