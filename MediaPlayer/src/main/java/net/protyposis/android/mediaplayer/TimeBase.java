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
