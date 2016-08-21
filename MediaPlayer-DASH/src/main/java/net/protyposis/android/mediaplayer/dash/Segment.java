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

/**
 * Created by maguggen on 27.08.2014.
 */
public class Segment {
    String media;
    String range;

    Segment() {
    }

    Segment(String media) {
        this.media = media;
    }

    Segment(String media, String range) {
        this(media);
        this.range = range;
    }

    public String getMedia() {
        return media;
    }

    public String getRange() {
        return range;
    }

    public boolean hasRange() {
        return range != null;
    }

    @Override
    public String toString() {
        return "Segment{" +
                "media='" + media + '\'' +
                ", range='" + range + '\'' +
                '}';
    }
}
