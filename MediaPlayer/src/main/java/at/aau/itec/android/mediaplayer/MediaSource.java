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
