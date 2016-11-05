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
import android.util.Log;

import java.io.IOException;
import java.util.Map;

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

    Call downloadAsync(Segment segment, CachedSegment cachedSegment, SegmentDownloadCallback callback) {
        Request request = buildSegmentRequest(segment);

        Call call = mHttpClient.newCall(request);
        call.enqueue(new ResponseCallback(cachedSegment, callback));

        return call;
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

    interface SegmentDownloadCallback {
        void onFailure(CachedSegment cachedSegment, IOException e);

        void onSuccess(CachedSegment cachedSegment, byte[] segmentData, long duration) throws IOException;
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
            mCallback.onFailure(mCachedSegment, e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
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

                    mCallback.onSuccess(mCachedSegment, segmentData, headerTime + payloadTime);
                } catch (IOException e) {
                    mCallback.onFailure(mCachedSegment, e);
                } finally {
                    response.body().close();
                }
            }
        }
    }
}
