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

import java.io.File;

/**
 * Created by Mario on 03.09.2014.
 */
class CachedSegment {
    int number;
    Segment segment;
    Representation representation;
    AdaptationSet adaptationSet;
    File file;
    long ptsOffsetUs;

    CachedSegment(int number, Segment segment, Representation representation, AdaptationSet adaptationSet) {
        this.number = number;
        this.segment = segment;
        this.representation = representation;
        this.adaptationSet = adaptationSet;
    }
}
