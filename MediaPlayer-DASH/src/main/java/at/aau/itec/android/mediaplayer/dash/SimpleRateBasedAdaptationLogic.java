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

import android.util.Log;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mario on 05.09.2014.
 */
public class SimpleRateBasedAdaptationLogic implements AdaptationLogic {

    private static final String TAG = SimpleRateBasedAdaptationLogic.class.getSimpleName();

    /* The running bandwidth average can be the same for all adaptation sets since they all
     * download their segments from the same network. */
    private RunningAverage mRunningAverage;

    private Map<AdaptationSet, AdaptationState> mStateMap;

    public SimpleRateBasedAdaptationLogic() {
        mRunningAverage = new RunningAverage(10);
        mStateMap = new HashMap<AdaptationSet, AdaptationState>();
    }

    private AdaptationState getState(AdaptationSet adaptationSet) {
        AdaptationState state = mStateMap.get(adaptationSet);
        if(state == null) {
            state = new AdaptationState();
            mStateMap.put(adaptationSet, state);
        }
        return state;
    }

    @Override
    public Representation initialize(AdaptationSet adaptationSet) {
        // sort representations by bandwidth ascending
        Collections.sort(adaptationSet.representations, new Comparator<Representation>() {
            @Override
            public int compare(Representation lhs, Representation rhs) {
                return lhs.bandwidth - rhs.bandwidth;
            }
        });

        return calculateRepresentation(adaptationSet);
    }

    @Override
    public void reportSegmentDownload(AdaptationSet adaptationSet, Representation representation,
                                      Segment segment, int byteSize, long downloadTimeMs) {
        int bandwidth = (int)(byteSize * 8 / (downloadTimeMs / 1000f));
        int averageBandwidth = mRunningAverage.next(bandwidth);
        Log.d(TAG, adaptationSet.getGroup() + " "
                + bandwidth + "bps current, "
                + averageBandwidth + " bps average");
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
         */
        AdaptationState state = getState(adaptationSet);
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
            newRepresentation = adaptationSet.representations.get(0);
        }

        if(state.currentRepresentation == null) {
            /* At the first calculation, the current representation is null and gets set to the
             * determined representation.
             */
            state.currentRepresentation = newRepresentation;
        } else {
            if(newRepresentation.bandwidth < state.currentRepresentation.bandwidth) {
                state.vote = -1;
            } else if(newRepresentation.bandwidth > state.currentRepresentation.bandwidth) {
                state.vote++;
            }

            /* If the vote is below zero, for what a singe downvote suffices, the adaptation
             * switches down to the fitting lower bandwidth representation.
             * If the there have been consecutive upvotes for at least 10 seconds in a row, the
             * adaptation switches to the fitting higher bandwidth representation.
             * Any other case does not make a change and the current representation is kept. */
            if(state.vote < 0 || state.vote >= Math.max(1, 10000000d / state.currentRepresentation.segmentDurationUs)) {
                Log.d(TAG, "vote=" + state.vote + " switch");
                state.currentRepresentation = newRepresentation;
                state.vote = 0;
            } else {
                newRepresentation = state.currentRepresentation;
            }
        }

        return newRepresentation;
    }

    private static class AdaptationState {
        private Representation currentRepresentation;
        private int vote;
    }

    private static class RunningAverage {

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
