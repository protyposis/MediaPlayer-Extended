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

import okhttp3.OkHttpClient;

public class DashSource extends UriSource {

    private OkHttpClient mHttpClient;
    private SegmentDownloader mSegmentDownloader;
    private AdaptationLogic mAdaptationLogic;
    private MPD mMPD;
    private int mCacheSizeInBytes = 100 * 1024 * 1024;

    public DashSource(Context context, Uri uri, OkHttpClient httpClient, Map<String, String> headers, AdaptationLogic adaptationLogic) {
        super(context, uri, headers);
        mHttpClient = httpClient;
        mAdaptationLogic = adaptationLogic;
        init();
    }

    public DashSource(Context context, Uri uri, Map<String, String> headers, AdaptationLogic adaptationLogic) {
        this(context, uri, null, headers, adaptationLogic);
    }

    public DashSource(Context context, Uri uri, OkHttpClient httpClient, AdaptationLogic adaptationLogic) {
        super(context, uri);
        mHttpClient = httpClient;
        mAdaptationLogic = adaptationLogic;
        init();
    }

    public DashSource(Context context, Uri uri, AdaptationLogic adaptationLogic) {
        this(context, uri, (OkHttpClient)null, adaptationLogic);
    }

    public DashSource(Context context, MPD mpd, OkHttpClient httpClient, AdaptationLogic adaptationLogic) {
        super(context, null);
        mMPD = mpd;
        mHttpClient = httpClient;
        mAdaptationLogic = adaptationLogic;
    }

    public DashSource(Context context, MPD mpd, AdaptationLogic adaptationLogic) {
        this(context, mpd, null, adaptationLogic);
    }

    private void initHttpClient() {
        // Create a http client instance if there is none yet
        if(mHttpClient == null) {
            mHttpClient = new OkHttpClient();
        }
        // Create a segment downloader if there is none yet
        if(mSegmentDownloader == null) {
            mSegmentDownloader = new SegmentDownloader(mHttpClient, getHeaders());
        }
    }

    private void init() {
        initHttpClient();
        if(mAdaptationLogic == null) {
            throw new RuntimeException("AdaptationLogic missing!");
        }
        if(getUri() != null) {
            try {
                mMPD = new DashParser().parse(this, mHttpClient);
            } catch (DashParserException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Gets the size of the segment cache. Default size is 100 megabytes.
     *
     * @return the size of the segment cache in bytes
     */
    public int getCacheSize() {
        return mCacheSizeInBytes;
    }

    /**
     * Sets the size of the segment cache. This only has an effect before the extractors are
     * created, i.e. before the DashSource is set as a data source (e.g. in MediaPlayer or VideoView).
     *
     * If this source has separate video and audio extractors, the used storage size may be twice
     * the configured cache size because each extractor has its own cache.
     *
     * If the size of the cache is smaller than the segments, segments are not cached and caching
     * is therefore disabled.
     *
     * @param sizeInBytes the size of the segment cache in bytes
     */
    public void setCacheSize(int sizeInBytes) {
        mCacheSizeInBytes = sizeInBytes;
    }

    @Override
    public MediaExtractor getVideoExtractor() throws IOException {
        initHttpClient(); // in case init() has not been called
        DashMediaExtractor mediaExtractor = new DashMediaExtractor();
        mediaExtractor.setCacheSize(mCacheSizeInBytes);
        mediaExtractor.setDataSource(getContext(), mMPD, mSegmentDownloader, mMPD.getFirstPeriod().getFirstVideoSet(), mAdaptationLogic);
        return mediaExtractor;
    }

    @Override
    public MediaExtractor getAudioExtractor() throws IOException {
        initHttpClient(); // in case init() has not been called
        AdaptationSet audioSet = mMPD.getFirstPeriod().getFirstAudioSet();
        if(audioSet != null){
            DashMediaExtractor mediaExtractor = new DashMediaExtractor();
            mediaExtractor.setCacheSize(mCacheSizeInBytes);
            mediaExtractor.setDataSource(getContext(), mMPD, mSegmentDownloader, audioSet, mAdaptationLogic);
            return mediaExtractor;
        } else {
            return null;
        }
    }
}