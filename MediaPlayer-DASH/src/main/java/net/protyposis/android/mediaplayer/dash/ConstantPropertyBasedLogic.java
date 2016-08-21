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
 * Does not do any adaptation and always recommends the same representation as
 * specified in the constructor.
 *
 * Created by Mario on 05.09.2014.
 */
public class ConstantPropertyBasedLogic implements AdaptationLogic {

    public enum Mode {
        LOWEST_BITRATE,
        HIGHEST_BITRATE
    }

    private Mode mMode;

    public ConstantPropertyBasedLogic(Mode mode) {
        mMode = mode;
    }

    @Override
    public Representation initialize(AdaptationSet adaptationSet) {
        return calculateRepresentation(adaptationSet);
    }

    @Override
    public void reportSegmentDownload(AdaptationSet adaptationSet, Representation representation, Segment segment, int byteSize, long downloadTimeMs) {

    }

    @Override
    public Representation getRecommendedRepresentation(AdaptationSet adaptationSet) {
        return calculateRepresentation(adaptationSet);
    }

    private Representation calculateRepresentation(AdaptationSet adaptationSet) {
        if(adaptationSet.representations.isEmpty()) {
            throw new RuntimeException("invalid state, an adaptation set must not be empty");
        }

        /* Under the assumption that the representations are always ordered by ascending bandwidth
         * in an MPD, the representation is solely chosen upon the index.
         * TODO order by bitrate to make sure
         */
        switch (mMode) {
            case LOWEST_BITRATE:
                return adaptationSet.representations.get(0);
            case HIGHEST_BITRATE:
                 return adaptationSet.representations.get(adaptationSet.representations.size() - 1);
            default:
                throw new RuntimeException("invalid state");
        }
    }
}
