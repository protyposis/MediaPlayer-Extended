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

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.Map;

import at.aau.itec.android.mediaplayer.MediaExtractor;
import at.aau.itec.android.mediaplayer.UriSource;

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