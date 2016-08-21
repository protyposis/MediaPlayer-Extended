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

package net.protyposis.android.mediaplayer.dash;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mario on 13.08.2015.
 */
public class Period {

    String id;
    long startUs;
    long durationUs;
    boolean bitstreamSwitching;
    List<AdaptationSet> adaptationSets;

    Period() {
        adaptationSets = new ArrayList<AdaptationSet>();
    }

    public List<AdaptationSet> getAdaptationSets() {
        return adaptationSets;
    }

    public AdaptationSet getFirstSetOfType(String mime) {
        for(AdaptationSet as : adaptationSets) {
            if(as.mimeType != null && as.mimeType.startsWith(mime)) {
                return as;
            } else {
                for(Representation r : as.representations) {
                    if(r.mimeType.startsWith(mime)) {
                        return as;
                    }
                }
            }
        }
        return null;
    }

    public AdaptationSet getFirstVideoSet() {
        return getFirstSetOfType("video/");
    }

    public AdaptationSet getFirstAudioSet() {
        return getFirstSetOfType("audio/");
    }
}
