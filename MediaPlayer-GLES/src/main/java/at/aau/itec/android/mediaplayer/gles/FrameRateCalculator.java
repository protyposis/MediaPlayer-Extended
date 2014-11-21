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

package at.aau.itec.android.mediaplayer.gles;

import android.os.SystemClock;
import android.util.Log;

/**
 * Created by maguggen on 10.07.2014.
 */
public class FrameRateCalculator {

    private static final String TAG = FrameRateCalculator.class.getSimpleName();

    private long[] mDurations;
    private long mDurationSum;
    private int mIndex;

    private long mLastFrameTime;

    public FrameRateCalculator(int movingAverageWindowSize) {
        mDurations = new long[movingAverageWindowSize];
        mLastFrameTime = SystemClock.elapsedRealtime();
    }

    public void frame() {
        long currentTime = SystemClock.elapsedRealtime();
        long duration = currentTime - mLastFrameTime;

        // go to next slot
        mIndex = (mIndex + 1) % mDurations.length;

        // the slot now contains the oldest duration which we subtract from the moving sum
        mDurationSum -= mDurations[mIndex];
        // then we add the current duration to the moving sum, and insert the duration also for later removal reference
        mDurationSum += duration;
        mDurations[mIndex] = duration;

        double avgFrameRate = 1000d / ((double) mDurationSum / mDurations.length);
        double currentFrameRate = 1000d / duration;

        Log.d(TAG, String.format("avg fps %.2f current fps %.2f", avgFrameRate, currentFrameRate));

        mLastFrameTime = currentTime;
    }
}
