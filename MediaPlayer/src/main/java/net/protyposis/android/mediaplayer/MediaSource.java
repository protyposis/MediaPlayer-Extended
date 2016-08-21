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

import java.io.IOException;

/**
 * Created by maguggen on 27.08.2014.
 */
public interface MediaSource {

    /**
     * Returns a media extractor for video data and possibly multiplexed audio data.
     */
    MediaExtractor getVideoExtractor() throws IOException;

    /**
     * Returns a media extractor for audio data from a separate audio stream, or NULL if the source
     * does not have a separate audio source or the audio is multiplexed with the video in a single
     * stream.
     */
    MediaExtractor getAudioExtractor() throws IOException;
}
