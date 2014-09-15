/*
 * Copyright (c) 2014 Mario Guggenberger <mario.guggenberger@aau.at>
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

package at.aau.itec.android.mediaplayer.dash;

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
