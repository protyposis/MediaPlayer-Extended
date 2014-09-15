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
                ", bandwidth=" + bandwidth +
                //", initSegment=" + initSegment +
                ", segmentDurationUs=" + segmentDurationUs +
                //", segments=" + segments +
                '}';
    }
}
