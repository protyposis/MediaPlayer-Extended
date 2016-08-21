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
