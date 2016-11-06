/*
 * Copyright 2016 Mario Guggenberger <mg@protyposis.net>
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

import android.os.SystemClock;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Mario on 05.11.2016.
 */

class SegmentDownloader {

    static final int INITSEGMENT = -1;

    private OkHttpClient mHttpClient;
    private Headers mHeaders;
    private PriorityQueue<CachedSegment> mDownloadQueue; // segments waiting in line to be requested
    private Map<String, Call> mDownloadRequests; // segments currently being requested
    private int mMaxConcurrentDownloadRequests = 3;

    SegmentDownloader(OkHttpClient httpClient, Map<String, String> headers) {
        if (httpClient == null) {
            throw new IllegalArgumentException("http client must be set");
        }

        mHttpClient = httpClient;

        Headers.Builder headersBuilder = new Headers.Builder();
        if (headers != null && !headers.isEmpty()) {
            for (String name : headers.keySet()) {
                headersBuilder.add(name, headers.get(name));
            }
        }
        mHeaders = headersBuilder.build();

        mDownloadQueue = new PriorityQueue<>(20, new Comparator<CachedSegment>() {
            @Override
            public int compare(CachedSegment lhs, CachedSegment rhs) {
                // Sort the downloads by their PTS (sorting by segment number fails when a/v segments are of different length)
                return (int)(lhs.ptsOffsetUs - rhs.ptsOffsetUs);
            }
        });
        mDownloadRequests = new HashMap<>();
    }

    SegmentDownloader(OkHttpClient httpClient) {
        this(httpClient, null);
    }

    Response downloadBlocking(Segment segment, Integer segmentNr) throws IOException {
        Request request = buildSegmentRequest(segment);
        Response response = mHttpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("sync dl error @ segment " + segmentNr + ": "
                    + response.code() + " " + response.message()
                    + " " + request.url().toString());
        }

        return response;
    }

    Call downloadAsync(CachedSegment cachedSegment, SegmentDownloadCallback callback) {
        Request request = buildSegmentRequest(cachedSegment.segment);

        Call call = mHttpClient.newCall(request);
        mDownloadRequests.put(getKey(cachedSegment.adaptationSet, cachedSegment.number), call);
        call.enqueue(new ResponseCallback(cachedSegment, callback));

        return call;
    }

    boolean isDownloading(AdaptationSet adaptationSet, int segmentNr) {
        // Check if the segment is in transfer
        if(mDownloadRequests.containsKey(getKey(adaptationSet, segmentNr))) {
            return true;
        }

        // Check if the segment is queued
        for(CachedSegment segment : mDownloadQueue) {
            if(segment.number == segmentNr && segment.adaptationSet == adaptationSet) {
                return true;
            }
        }

        return false;
    }

    void cancelDownloads() {
        // Clear waiting queue
        mDownloadQueue.clear();

        // Cancel requests
        for(String key : mDownloadRequests.keySet()) {
            mDownloadRequests.get(key).cancel();
        }
        mDownloadRequests.clear();
    }

    /**
     * Returns a unique key for a segment of an adaptation set. Just using the segment number as
     * key does not suffice because multiple adaptation sets (e.g. video and audio) have overlapping
     * segment numbers.
     */
    private String getKey(AdaptationSet adaptationSet, int segmentNr) {
        return adaptationSet.group + "-" + segmentNr;
    }

    /**
     * Builds a request object for a segment.
     */
    private Request buildSegmentRequest(Segment segment) {
        // Replace illegal special chars
        String url = segment.media
                .replace(" ", "%20") // space
                .replace("^", "%5E"); // circumflex

        Request.Builder builder = new Request.Builder().url(url).headers(mHeaders);

        if (segment.hasRange()) {
            builder.addHeader("Range", "bytes=" + segment.range);
        }

        return builder.build();
    }

    class DownloadFinishedArgs {

        CachedSegment cachedSegment;
        byte[] data;
        long duration;

        DownloadFinishedArgs(CachedSegment cachedSegment, byte[] data, long duration) {
            this.cachedSegment = cachedSegment;
            this.data = data;
            this.duration = duration;
        }
    }

    interface SegmentDownloadCallback {
        void onFailure(CachedSegment cachedSegment, IOException e);
        void onSuccess(DownloadFinishedArgs args) throws IOException;
    }

    private class ResponseCallback implements Callback {

        private CachedSegment mCachedSegment;
        private SegmentDownloadCallback mCallback;

        ResponseCallback(CachedSegment cachedSegment, SegmentDownloadCallback callback) {
            mCachedSegment = cachedSegment;
            mCallback = callback;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            mDownloadRequests.remove(mCachedSegment.number);
            if(!call.isCanceled()) {
                // Call back only if a request 'really' failed, i.e. if it hasn't been canceled on purpose
                mCallback.onFailure(mCachedSegment, e);
            }
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            mDownloadRequests.remove(mCachedSegment.number);
            if (response.isSuccessful()) {
                try {
                    long startTime = SystemClock.elapsedRealtime();
                    byte[] segmentData = response.body().bytes();

                    /* The time it takes to send the request header to the server until the response
                     * headers arrive. Can be custom implemented through an Interceptor too, in case
                     * this should ever fail in the future. */
                    long headerTime = response.receivedResponseAtMillis() - response.sentRequestAtMillis();

                    /* The time it takes to read the result body, which is the actual segment data.
                     * The sum of this time together with the header time is the total segment download time. */
                    long payloadTime = SystemClock.elapsedRealtime() - startTime;

                    mCallback.onSuccess(new DownloadFinishedArgs(mCachedSegment, segmentData, headerTime + payloadTime));
                } catch (IOException e) {
                    mCallback.onFailure(mCachedSegment, e);
                } finally {
                    response.body().close();
                }
            }
        }
    }
}
