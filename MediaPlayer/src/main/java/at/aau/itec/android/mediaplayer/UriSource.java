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

package at.aau.itec.android.mediaplayer;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.Map;

/**
 * Created by maguggen on 26.08.2014.
 */
public class UriSource implements MediaSource {

    private Context mContext;
    private Uri mUri;
    private Map<String, String> mHeaders;

    public UriSource(Context context, Uri uri, Map<String, String> headers) {
        this.mContext = context;
        this.mUri = uri;
        this.mHeaders = headers;
    }

    public UriSource(Context context, Uri uri) {
        this.mContext = context;
        this.mUri = uri;
    }

    public Context getContext() {
        return mContext;
    }

    public Uri getUri() {
        return mUri;
    }

    public Map<String, String> getHeaders() {
        return mHeaders;
    }

    @Override
    public MediaExtractor getVideoExtractor() throws IOException {
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(mContext, mUri, mHeaders);
        return mediaExtractor;
    }

    @Override
    public MediaExtractor getAudioExtractor() throws IOException {
        return null; // UriSource does only handle single (multiplexed) files
    }
}
