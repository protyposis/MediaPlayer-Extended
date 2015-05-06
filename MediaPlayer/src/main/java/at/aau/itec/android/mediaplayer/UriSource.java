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
    private Uri mAudioUri;
    private Map<String, String> mHeaders;
    private Map<String, String> mAudioHeaders;

    /**
     * Creates a media source from a URI. The media source must either be a video stream
     * or a multiplexed audio/video stream.
     * @param context the context to open the URI in
     * @param uri the URI pointing to the media source
     * @param headers the headers to be passed with the request to the URI
     */
    public UriSource(Context context, Uri uri, Map<String, String> headers) {
        this.mContext = context;
        this.mUri = uri;
        this.mHeaders = headers;
    }

    /**
     * Creates a media source from a URI. The media source must either be a video stream
     * or a multiplexed audio/video stream.
     * @param context the context to open the URI in
     * @param uri the URI pointing to the media source
     */
    public UriSource(Context context, Uri uri) {
        this.mContext = context;
        this.mUri = uri;
    }

    /**
     * Creates a media source from separate video and audio URIs.
     * @param context the context to open the URIs in
     * @param videoUri the URI pointing to the video source
     * @param videoHeaders the headers to be passed with the request to the video URI
     * @param audioUri the URI pointing to the audio source
     * @param audioHeaders the headers to be passed with the request to the audio URI
     */
    public UriSource(Context context, Uri videoUri, Map<String, String> videoHeaders, Uri audioUri, Map<String, String> audioHeaders) {
        this.mContext = context;
        this.mUri = videoUri;
        this.mHeaders = videoHeaders;
        this.mAudioUri = audioUri;
        this.mAudioHeaders = audioHeaders;
    }

    /**
     * Creates a media source from separate video and audio URIs.
     * @param context the context to open the URIs in
     * @param videoUri the URI pointing to the video source
     * @param audioUri the URI pointing to the audio source
     */
    public UriSource(Context context, Uri videoUri, Uri audioUri) {
        this.mContext = context;
        this.mUri = videoUri;
        this.mAudioUri = audioUri;
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
        if(mAudioUri != null) {
            // In case of a separate audio file Uri, return an audio extractor
            MediaExtractor mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(mContext, mAudioUri, mAudioHeaders);
            return mediaExtractor;
        }
        // We do not need a separate audio extractor when only a single Uri to a single
        // (multiplexed) file is passed into this class.
        return null;
    }
}
