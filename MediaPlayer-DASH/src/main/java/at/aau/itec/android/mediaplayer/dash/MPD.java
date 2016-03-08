/*
 * Copyright (c) 2014 Mario Guggenberger <mg@protyposis.net>
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
    List<Period> periods;

    MPD() {
        periods = new ArrayList<Period>();
    }

    public long getMediaPresentationDurationUs() {
        return mediaPresentationDurationUs;
    }

    public long getMinBufferTimeUs() {
        return minBufferTimeUs;
    }

    public List<Period> getPeriods() {
        return periods;
    }

    public Period getFirstPeriod() {
        if(!periods.isEmpty()) {
            return periods.get(0);
        }
        return null;
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
