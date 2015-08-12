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

package at.aau.itec.android.mediaplayer.dash;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by maguggen on 28.08.2014.
 */
public class MPD {

    boolean isDynamic;
    long mediaPresentationDurationUs;
    Date availabilityStartTime;
    long timeShiftBufferDepthUs;
    long suggestedPresentationDelayUs;
    long maxSegmentDurationUs;
    long minBufferTimeUs;
    List<AdaptationSet> adaptationSets;

    MPD() {
        adaptationSets = new ArrayList<AdaptationSet>();
    }

    public long getMediaPresentationDurationUs() {
        return mediaPresentationDurationUs;
    }

    public long getMinBufferTimeUs() {
        return minBufferTimeUs;
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

    @Override
    public String toString() {
        return "MPD{" +
                "mediaPresentationDurationUs=" + mediaPresentationDurationUs +
                ", minBufferTimeUs=" + minBufferTimeUs +
                //", representations=" + representations +
                '}';
    }
}
