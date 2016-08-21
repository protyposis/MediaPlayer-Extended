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

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.Map;

import net.protyposis.android.mediaplayer.MediaExtractor;
import net.protyposis.android.mediaplayer.UriSource;

public class DashSource extends UriSource {

    private AdaptationLogic mAdaptationLogic;
    private MPD mMPD;

    public DashSource(Context context, Uri uri, Map<String, String> headers, AdaptationLogic adaptationLogic) {
        super(context, uri, headers);
        mAdaptationLogic = adaptationLogic;
        init();
    }

    public DashSource(Context context, Uri uri, AdaptationLogic adaptationLogic) {
        super(context, uri);
        mAdaptationLogic = adaptationLogic;
        init();
    }

    public DashSource(Context context, MPD mpd, AdaptationLogic adaptationLogic) {
        super(context, null);
        mMPD = mpd;
        mAdaptationLogic = adaptationLogic;
    }

    private void init() {
        if(mAdaptationLogic == null) {
            throw new RuntimeException("AdaptationLogic missing!");
        }
        if(getUri() != null) {
            try {
                mMPD = new DashParser().parse(this);
            } catch (DashParserException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public MediaExtractor getVideoExtractor() throws IOException {
        DashMediaExtractor mediaExtractor = new DashMediaExtractor();
        mediaExtractor.setDataSource(getContext(), mMPD, mMPD.getFirstPeriod().getFirstVideoSet(), mAdaptationLogic);
        return mediaExtractor;
    }

    @Override
    public MediaExtractor getAudioExtractor() throws IOException {
        AdaptationSet audioSet = mMPD.getFirstPeriod().getFirstAudioSet();
        if(audioSet != null){
            DashMediaExtractor mediaExtractor = new DashMediaExtractor();
            mediaExtractor.setDataSource(getContext(), mMPD, audioSet, mAdaptationLogic);
            return mediaExtractor;
        } else {
            return null;
        }
    }
}