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

import android.util.Log;

/**
 * Created by Mario on 05.09.2014.
 */
public class SimpleRateBasedAdaptationLogic implements AdaptationLogic {

    private static final String TAG = SimpleRateBasedAdaptationLogic.class.getSimpleName();

    private RunningAverage mRunningAverage;

    public SimpleRateBasedAdaptationLogic() {
        mRunningAverage = new RunningAverage(10);
    }

    @Override
    public Representation initialize(AdaptationSet adaptationSet) {
        return calculateRepresentation(adaptationSet);
    }

    @Override
    public void reportSegmentDownload(AdaptationSet adaptationSet, Representation representation, Segment segment, int byteSize, long downloadTimeMs) {
        int bandwidth = (int)(byteSize * 8 / (downloadTimeMs / 1000f));
        int averageBandwidth = mRunningAverage.next(bandwidth);
        Log.d(TAG, bandwidth + "bps current, " + averageBandwidth + " bps average");
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
        int averageBandwidth = mRunningAverage.average();
        Representation newRepresentation = null;
        for(Representation representation : adaptationSet.representations) {
            if(representation.bandwidth <= averageBandwidth) {
                newRepresentation = representation;
            } else {
                break;
            }
        }
        if(newRepresentation == null) {
            /* When all representations require more bandwidth than currently available,
             * the lowest representation is selected.
             */
            newRepresentation = adaptationSet.representations.get(adaptationSet.representations.size()-1);
        }

        return newRepresentation;
    }

    public static class RunningAverage {

        private int[] values;
        private int fillLevel;
        private int index;
        private int averageSum;

        /**
         * Creates a running average with the specified count. A count of 5 means an average over
         * the last 5 added values.
         */
        public RunningAverage(int count) {
            values = new int[count];
            reset();
        }

        public void reset() {
            index = -1;
            fillLevel = 0;
            averageSum = 0;
        }

        /**
         * Adds a new value and returns the new average.
         */
        public int next(int value) {
            if(fillLevel < values.length) {
                fillLevel++;
            }
            index = (index + 1) % values.length;
            int oldestIndex = positiveMod((index - values.length), values.length);
            averageSum -= values[oldestIndex];
            values[index] = value;
            averageSum += value;
            return average();
        }

        /**
         * Returns the current average.
         */
        public int average() {
            if(fillLevel == 0) {
                return 0;
            }
            return averageSum / fillLevel;
        }

        /**
         * Shifts negative modulo results to the positive to always get a valid array index.
         */
        public static int positiveMod(int m, int n) {
            int mod = m % n;
            if (mod < 0) mod += n;
            return mod;
        }
    }
}
