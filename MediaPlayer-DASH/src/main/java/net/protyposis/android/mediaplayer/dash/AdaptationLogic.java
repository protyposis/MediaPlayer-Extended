/*
 * Copyright (c) 2014 Mario Guggenberger <mg@protyposis.net>
 *
 * This file is part of MediaPlayer-Extended.
 *
 * MediaPlayer-Extended is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MediaPlayer-Extended is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MediaPlayer-Extended.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.protyposis.android.mediaplayer.dash;

/**
 * This class receives performance data on downloaded segments, does some internal magic with it
 * and recommends the best fitting representation for a given adaptation set.
 *
 * Created by maguggen on 26.08.2014.
 */
public interface AdaptationLogic {

    /**
     * Returns an initial adaptation set to start, before any segments have been loaded.
     */
    Representation initialize(AdaptationSet adaptationSet);

    /**
     * Receiver of performance data on downloaded segments in the {@link net.protyposis.android.mediaplayer.dash.DashMediaExtractor}.
     */
    void reportSegmentDownload(AdaptationSet adaptationSet, Representation representation, Segment segment, int byteSize, long downloadTimeMs);

    /**
     * Returns the recommended representation at the time of calling.
     */
    Representation getRecommendedRepresentation(AdaptationSet adaptationSet);
}
