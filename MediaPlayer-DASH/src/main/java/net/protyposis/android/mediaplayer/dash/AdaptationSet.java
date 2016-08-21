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
 * Created by maguggen on 28.08.2014.
 */
public class AdaptationSet {

    int group;
    String mimeType;
    int maxWidth;
    int maxHeight;
    float par; // picture aspect ratio (also called DAR - display aspect ratio)
    List<Representation> representations;

    AdaptationSet() {
        representations = new ArrayList<Representation>();
    }

    public int getGroup() {
        return group;
    }

    public String getMimeType() {
        return mimeType;
    }

    public List<Representation> getRepresentations() {
        return representations;
    }

    public boolean hasMaxDimensions() {
        return maxWidth > 0 && maxHeight > 0;
    }

    public boolean hasPAR() {
        return par > 0;
    }

    @Override
    public String toString() {
        return "AdaptationSet{" +
                "group=" + group +
                ", mimeType='" + mimeType + '\'' +
                ", maxWidth='" + maxWidth +
                ", maxHeight='" + maxHeight +
                ", par='" + par +
                //", representations=" + representations +
                '}';
    }
}
