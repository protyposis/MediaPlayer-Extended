/*
 * Copyright 2018 Mario Guggenberger <mg@protyposis.net>
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
 * Created by Mario on 15.03.2018.
 */

public class Cue {

    private int time;
    private Object data;

    Cue(int time, Object data) {
        this.time = time;
        this.data = data;
    }

    /**
     * Gets the time at which this cue is cued. This must not necessarily be the exact playback
     * time as cue events can be slightly delayed. Use {@link MediaPlayer#getCurrentPosition()}
     * to get the current playback time instead.
     * @return the time at which this cue is cued
     */
    public int getTime() {
        return time;
    }

    /**
     * Gets the custom data object attached to this cue.
     * @return the data attached to this cue
     */
    public Object getData() {
        return data;
    }

    /**
     * Checks if this cue has data attached.
     * @return true if this cue has data attached, else false
     */
    public boolean hasData() {
        return data != null;
    }

    @Override
    public String toString() {
        return "Cue{" +
                "time=" + time +
                ", data=" + data +
                '}';
    }
}
