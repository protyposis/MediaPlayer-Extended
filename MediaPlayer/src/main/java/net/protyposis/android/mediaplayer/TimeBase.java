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

package net.protyposis.android.mediaplayer;

/**
 * Created by Mario on 14.06.2014.
 *
 * A time base in microseconds for media playback.
 */
class TimeBase {

    private long mStartTime;
    private double mSpeed = 1.0;

    public TimeBase() {
        start();
    }

    public void start() {
        startAt(0);
    }

    public void startAt(long mediaTime) {
        mStartTime = microTime() - mediaTime;
    }

    public long getCurrentTime() {
        return microTime() - mStartTime;
    }

    public long getOffsetFrom(long from) {
        return  from - getCurrentTime();
    }

    public double getSpeed() {
        return mSpeed;
    }

    /**
     * Sets the playback speed. Can be used for fast forward and slow motion.
     * speed 0.5 = half speed / slow motion
     * speed 2.0 = double speed / fast forward
     * @param speed
     */
    public void setSpeed(double speed) {
        mSpeed = speed;
    }

    private long microTime() {
        return (long)(System.nanoTime() / 1000 * mSpeed);
    }
}
