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
import java.util.List;

/**
 * Created by maguggen on 27.08.2014.
 */
public class Representation {
    String id;
    String codec;
    String mimeType;
    int width; // pixels
    int height; // pixels
    float sar; // storage aspect ratio
    int bandwidth; // bits/sec

    long segmentDurationUs;
    Segment initSegment;
    List<Segment> segments;

    Representation() {
        segments = new ArrayList<Segment>();
    }

    public String getId() {
        return id;
    }

    public String getCodec() {
        return codec;
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean hasSAR() {
        return sar > 0;
    }

    public float calculatePAR() {
        float sizeRatio = (float) width / height;
        return sizeRatio * (hasSAR() ? sar : 1);
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public long getSegmentDurationUs() {
        return segmentDurationUs;
    }

    public Segment getInitSegment() {
        return initSegment;
    }

    public List<Segment> getSegments() {
        return segments;
    }

    boolean hasSegments() {
        return !segments.isEmpty();
    }

    Segment getLastSegment() {
        return segments.get(segments.size() - 1);
    }

    @Override
    public String toString() {
        return "Representation{" +
                "id=" + id +
                ", codec='" + codec + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", dar=" + sar +
                ", bandwidth=" + bandwidth +
                //", initSegment=" + initSegment +
                ", segmentDurationUs=" + segmentDurationUs +
                //", segments=" + segments +
                '}';
    }
}
