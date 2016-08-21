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
