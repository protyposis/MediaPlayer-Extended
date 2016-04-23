/*
 * Copyright (c) 2014 Mario Guggenberger <mg@protyposis.net>
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
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.TrackBox;
import com.googlecode.mp4parser.MemoryDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Mp4TrackImpl;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.boxes.threegpp26244.SegmentIndexBox;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.http.OkHeaders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import at.aau.itec.android.mediaplayer.MediaExtractor;
import okio.BufferedSink;
import okio.ByteString;
import okio.Okio;

/**
 * Encapsulates DASH data source processing. The Android API's MediaExtractor doesn't support
 * switching between / chaining of multiple data sources, e.g. an initialization segment and a
 * succeeding media data segment. This class takes care of DASH file downloads, merging init data
 * with media file segments, chaining multiple files and switching between them.
 *
 * From outside, it looks like it is processing a single data source, similar to the Android API MediaExtractor.
 *
 * Created by maguggen on 27.08.2014.
 */
class DashMediaExtractor extends MediaExtractor {

    private static final String TAG = DashMediaExtractor.class.getSimpleName();

    private static volatile int sInstanceCount = 0;

    private Context mContext;
    private MPD mMPD;
    private AdaptationLogic mAdaptationLogic;
    private AdaptationSet mAdaptationSet;
    private Representation mRepresentation;
    private long mMinBufferTimeUs;
    private boolean mRepresentationSwitched;
    private int mCurrentSegment;
    private List<Integer> mSelectedTracks;
    private OkHttpClient mHttpClient;
    private Map<Representation, ByteString> mInitSegments;
    private Map<Integer, CachedSegment> mFutureCache; // the cache for upcoming segments
    private Map<Integer, Call> mFutureCacheRequests; // requests for upcoming segments
    private SegmentLruCache mUsedCache; // cache for used or in use segments
    private boolean mMp4Mode;
    private DefaultMp4Builder mMp4Builder;
    private long mSegmentPTSOffsetUs;

    public DashMediaExtractor() {
        mHttpClient = new OkHttpClient();
    }

    public final void setDataSource(Context context, MPD mpd, AdaptationSet adaptationSet,
                                    AdaptationLogic adaptationLogic)
            throws IOException {
        try {
            mContext = context;
            mMPD = mpd;
            mAdaptationSet = adaptationSet;
            mAdaptationLogic = adaptationLogic;
            mRepresentation = adaptationLogic.initialize(mAdaptationSet);
            mMinBufferTimeUs = Math.max(mMPD.minBufferTimeUs, 10 * 1000000L); // 10 secs min buffer time
            mCurrentSegment = -1;
            mSelectedTracks = new ArrayList<>();
            mInitSegments = new ConcurrentHashMap<>(mAdaptationSet.representations.size());
            mFutureCache = new ConcurrentHashMap<>();
            mFutureCacheRequests = new HashMap<>();
            mUsedCache = new SegmentLruCache(100 * 1024 * 1024);
            mMp4Mode = mRepresentation.mimeType.equals("video/mp4") || mRepresentation.initSegment.media.endsWith(".mp4");
            if (mMp4Mode) {
                mMp4Builder = new DefaultMp4Builder();
            }
            mSegmentPTSOffsetUs = 0;

            /* If the extractor previously crashed and could not gracefully finish, some old temp files
             * that will never be used again might be around, so just delete all of them and avoid the
             * memory fill up with trash.
             * Only clean at startup of the first instance, else newer ones delete cache files of
             * running ones.
             */
            if (sInstanceCount++ == 0) {
                clearTempDir(mContext);
            }

            initOnWorkerThread(getNextSegment());
        } catch (Exception e) {
            Log.e(TAG, "failed to set data source");
            throw new IOException("failed to set data source", e);
        }
    }

    @Override
    public MediaFormat getTrackFormat(int index) {
        MediaFormat mediaFormat = super.getTrackFormat(index);
        if(mMp4Mode) {
            /* An MP4 that has been converted from a fragmented to an unfragmented container
             * through the isoparser library does only contain the current segment's runtime. To
             * return the total runtime, we take the value from the MPD instead.
             */
            mediaFormat.setLong(MediaFormat.KEY_DURATION, mMPD.mediaPresentationDurationUs);
        }
        if(mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
            // Return the display aspect ratio as defined in the MPD (can be different from the encoded video size)
            mediaFormat.setFloat(MEDIA_FORMAT_EXTENSION_KEY_DAR,
                    mAdaptationSet.hasPAR() ? mAdaptationSet.par : mRepresentation.calculatePAR());
        }
        return mediaFormat;
    }

    @Override
    public void selectTrack(int index) {
        super.selectTrack(index);
        mSelectedTracks.add(index); // save track selection for later reinitialization
    }

    @Override
    public void unselectTrack(int index) {
        super.unselectTrack(index);
        mSelectedTracks.remove(Integer.valueOf(index));
    }

    @Override
    public int getSampleTrackIndex() {
        int index = super.getSampleTrackIndex();
        if(index == -1) {
            /* EOS of current segment reached. Check for and read from successive segment if
             * existing, else return the EOS flag. */
            try {
                if (switchToNextSegment()) {
                    return super.getSampleTrackIndex();
                }
            } catch (IOException e) {
                Log.e(TAG, "segment switching failed", e);
            }
        }
        return index;
    }

    @Override
    public int readSampleData(ByteBuffer byteBuf, int offset) {
        int size = super.readSampleData(byteBuf, offset);
        if(size == -1) {
            /* EOS of current segment reached. Check for and read from successive segment if
             * existing, else return the EOS flag. */
            try {
                if (switchToNextSegment()) {
                    /* If the representation switches during this read call, we cannot continue reading
                     * data from the next segment, because the video codec needs to reinitialize before.
                     * Else, some data is first fed into the decoder and then it is reinitialized, which
                     * results in skipped (sync) frames and artifacts.
                     * By returning 0, the decoder has time to check if the representation has changed,
                     * reconfigure itself and then issue another read. */
                    if (mRepresentationSwitched) {
                        return 0;
                    } else {
                        return super.readSampleData(byteBuf, offset);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "segment switching failed", e);
            }
        }
        return size;
    }

    /**
     * Tries to switch to the next segment and returns true if there is one, false if there is none
     * and thus the current is the last one.
     */
    private boolean switchToNextSegment() throws IOException {
        Integer next = getNextSegment();
        if(next != null) {
            /* Since it seems that an extractor cannot be reused by setting another data source,
             * a new instance needs to be created and used. */
            renewExtractor();

            /* Initialize the new extractor for the next segment */
            initOnWorkerThread(next);

            return true;
        }

        return false;
    }

    @Override
    public long getCachedDuration() {
        return mFutureCache.size() * mRepresentation.segmentDurationUs;
    }

    @Override
    public boolean hasCacheReachedEndOfStream() {
        /* The cache has reached EOS,
         * either if the last segment is in the future cache,
         * or of the last segment is currently played back.
         */
        return mFutureCache.containsKey(mRepresentation.getLastSegment())
                || mCurrentSegment == (mRepresentation.segments.size() - 1);
    }

    @Override
    public long getSampleTime() {
        long sampleTime = super.getSampleTime();
        if(sampleTime == -1) {
            return -1;
        } else {
            //Log.d(TAG, "sampletime = " + (sampleTime + mSegmentPTSOffsetUs))
            return sampleTime + mSegmentPTSOffsetUs;
        }
    }

    @Override
    public void seekTo(long timeUs, int mode) throws IOException {
        int targetSegmentIndex = Math.min((int) (timeUs / mRepresentation.segmentDurationUs), mRepresentation.segments.size() - 1);
        Log.d(TAG, "seek to " + timeUs + " @ segment " + targetSegmentIndex);
        if(targetSegmentIndex == mCurrentSegment) {
            /* Because the DASH segments do not contain seeking cues, the position in the current
             * segment needs to be reset to the start. Else, seeks are always progressing, never
             * going back in time. */
            super.seekTo(0, mode);
        } else {
            invalidateFutureCache();
            renewExtractor();
            mCurrentSegment = targetSegmentIndex;
            initOnWorkerThread(targetSegmentIndex);
            super.seekTo(timeUs - mSegmentPTSOffsetUs, mode);
        }
    }

    @Override
    public void release() {
        super.release();
        invalidateFutureCache();
        mUsedCache.evictAll();
    }

    /**
     * Informs the caller if the representation has changed, and resets the flag. This means, it
     * returns true only once (the first time) after the representation has changed.
     * @return true if the representation has changed between the previous and current call, else false
     */
    @Override
    public boolean hasTrackFormatChanged() {
        if(mRepresentationSwitched) {
            mRepresentationSwitched = false;
            return true;
        }
        return false;
    }

    private void initOnWorkerThread(final Integer segmentNr) throws IOException {
        /* Avoid NetworkOnMainThreadException by running network request in worker thread
         * but blocking until finished to avoid complicated and in this case unnecessary
         * async handling.
         */
        try {
            final Exception[] asyncException = new Exception[1];

            String currentThreadName = Thread.currentThread().getName();
            if(currentThreadName != null && currentThreadName.startsWith("AsyncTask")) {
                // We are already inside an async task, just continue on this thread
                try {
                    init(segmentNr);
                } catch (Exception e) {
                    asyncException[0] = e;
                }
            } else {
                // We are on the main thread, execute asynchronously and wait for the result
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                        try {
                            init(segmentNr);
                        } catch (Exception e) {
                            asyncException[0] = e;
                        }
                        return null;
                    }
                }.execute().get();
            }

            // hand an async thrown exception over to the main thread
            if(asyncException[0] != null) {
                throw new IOException("error initializing segment", asyncException[0]);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void init(Integer segmentNr) throws IOException {
        // Check for segment in caches, and execute blocking download if missing
        // First, check the future cache, without a seek the chance is much higher of finding it there
        CachedSegment cachedSegment = mFutureCache.remove(segmentNr);
        if(cachedSegment == null) {
            // Second, check the already used cache, maybe we had a seek and the segment is already there
            cachedSegment = mUsedCache.get(segmentNr);
            if(cachedSegment == null) {
                // Third, check if a request is already active
                Call call = mFutureCacheRequests.get(segmentNr);
                /* TODO add synchronization to the whole caching code
                 * E.g., a request could have finished between this mFutureCacheRequests call and
                 * the previous mUsedCache call, whose result is missed.
                 */
                if(call != null) {
                    synchronized (mFutureCache) {
                        try {
                            while((cachedSegment = mFutureCache.remove(segmentNr)) == null) {
                                Log.d(TAG, "waiting for request to finish " + segmentNr);
                                mFutureCache.wait();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    // Fourth, least and worst alternative: blocking download of segment
                    cachedSegment = downloadFile(segmentNr);
                }
            }
        }

        mUsedCache.put(segmentNr, cachedSegment);
        mSegmentPTSOffsetUs = cachedSegment.ptsOffsetUs;
        setDataSource(cachedSegment.file.getPath());

        // Reselect tracks at reinitialization for a successive segment
        if(!mSelectedTracks.isEmpty()) {
            for(int index : mSelectedTracks) {
                super.selectTrack(index);
            }
        }

        // Switch representation
        if(cachedSegment.representation != mRepresentation) {
            //invalidateFutureCache();
            Log.d(TAG, "representation switch: " + mRepresentation + " -> " + cachedSegment.representation);
            mRepresentationSwitched = true;
            mRepresentation = cachedSegment.representation;
        }

        // Switch future caching to the currently best representation
        Representation recommendedRepresentation = mAdaptationLogic.getRecommendedRepresentation(mAdaptationSet);
        fillFutureCache(recommendedRepresentation);
    }

    private Integer getNextSegment() {
        mCurrentSegment++;

        if(mRepresentation.segments.size() <= mCurrentSegment) {
            return null; // EOS, no more segment
        }

        return mCurrentSegment;
    }

    /**
     * Blocking download of a segment.
     */
    private CachedSegment downloadFile(Integer segmentNr) throws IOException {
        // At the first call, download the initialization segments, and reuse them later.
        if(mInitSegments.isEmpty()) {
            for(Representation representation : mAdaptationSet.representations) {
                Request request = buildSegmentRequest(representation.initSegment);
                long startTime = SystemClock.elapsedRealtime();
                Response response = mHttpClient.newCall(request).execute();
                if(!response.isSuccessful()) {
                    throw new IOException("sync dl error @ init segment: "
                            + response.code() + " " + response.message()
                            + " " + request.url().toString());
                }
                ByteString segmentData = response.body().source().readByteString();
                mInitSegments.put(representation, segmentData);
                mAdaptationLogic.reportSegmentDownload(mAdaptationSet, representation, representation.segments.get(segmentNr), segmentData.size(), SystemClock.elapsedRealtime() - startTime);
                Log.d(TAG, "init " + representation.initSegment.toString());
            }
        }

        Segment segment = mRepresentation.segments.get(segmentNr);
        Request request = buildSegmentRequest(segment);
        long startTime = SystemClock.elapsedRealtime();
        Response response = mHttpClient.newCall(request).execute();
        if(!response.isSuccessful()) {
            throw new IOException("sync dl error @ segment " + segmentNr + ": "
                    + response.code() + " " + response.message()
                    + " " + request.url().toString());
        }
        byte[] segmentData = response.body().bytes();
        mAdaptationLogic.reportSegmentDownload(mAdaptationSet, mRepresentation, segment, segmentData.length, SystemClock.elapsedRealtime() - startTime);
        CachedSegment cachedSegment = new CachedSegment(segmentNr, segment, mRepresentation);
        handleSegment(segmentData, cachedSegment);
        Log.d(TAG, "sync dl " + segmentNr + " " + segment.toString() + " -> " + cachedSegment.file.getPath());

        return cachedSegment;
    }

    /**
     * Makes async segment requests to fill the cache up to a certain level.
     */
    private synchronized void fillFutureCache(Representation representation) {
        int segmentsToBuffer = (int)Math.ceil((double)mMinBufferTimeUs / mRepresentation.segmentDurationUs);
        for(int i = mCurrentSegment + 1; i < Math.min(mCurrentSegment + 1 + segmentsToBuffer, mRepresentation.segments.size()); i++) {
            if(!mFutureCache.containsKey(i) && !mFutureCacheRequests.containsKey(i)) {
                Segment segment = representation.segments.get(i);
                Request request = buildSegmentRequest(segment);
                Call call = mHttpClient.newCall(request);
                CachedSegment cachedSegment = new CachedSegment(i, segment, representation); // segment could be accessed through representation by i
                call.enqueue(new SegmentDownloadCallback(cachedSegment));
                mFutureCacheRequests.put(i, call);
            }
        }
    }

    /**
     * Invalidates the cache by cancelling all pending requests and deleting all buffered segments.
     */
    private synchronized void invalidateFutureCache() {
        // cancel and remove requests
        for(Integer segmentNumber : mFutureCacheRequests.keySet()) {
            mFutureCacheRequests.get(segmentNumber).cancel();
        }
        mFutureCacheRequests.clear();

        // delete and remove files
        for(Integer segmentNumber : mFutureCache.keySet()) {
            mFutureCache.get(segmentNumber).file.delete();
        }
        mFutureCache.clear();
    }

    /**
     * http://developer.android.com/training/basics/data-storage/files.html
     */
    private File getTempFile(Context context, String fileName) {
        File file = null;
        try {
            fileName = fileName.replaceAll("\\W+", ""); // remove all special chars to get a valid filename
            file = File.createTempFile(fileName, null, context.getCacheDir());
        } catch (IOException e) {
            // Error while creating file
        }
        return file;
    }

    private void clearTempDir(Context context) {
        for(File file : context.getCacheDir().listFiles()) {
            file.delete();
        }
    }

    /**
     * Builds a request object for a segment.
     */
    private Request buildSegmentRequest(Segment segment) {
        // Replace illegal special chars
        String url = segment.media
                .replace(" ", "%20") // space
                .replace("^", "%5E"); // circumflex

        Request.Builder builder = new Request.Builder().url(url);

        if(segment.hasRange()) {
            builder.addHeader("Range", "bytes=" + segment.range);
        }

        return builder.build();
    }

    /**
     * Handles a segment by merging it with the init segment into a temporary file.
     */
    private void handleSegment(byte[] mediaSegment, CachedSegment cachedSegment) throws IOException {
        File segmentFile = getTempFile(mContext, "seg" + cachedSegment.representation.id + "-" + cachedSegment.segment.range + "");
        long segmentPTSOffsetUs = 0;

        if(mMp4Mode) {
            /* The MP4 iso format needs special treatment because the Android MediaExtractor/MediaCodec
             * does not support the fragmented MP4 container format. Each segment therefore needs
             * to be joined with the init fragment and converted to a "conventional" unfragmented MP4
             * container file. */
            IsoFile baseIsoFile = new IsoFile(new MemoryDataSourceImpl(mInitSegments.get(cachedSegment.representation).toByteArray())); // TODO do not go ByteString -> byte[] -> ByteBuffer, find more efficient way (custom mp4parser DataSource maybe?)
            IsoFile fragment = new IsoFile(new MemoryDataSourceImpl(mediaSegment));

            /* The PTS in a converted MP4 always start at 0, so we read the offset from the segment
             * index box and work with it at the necessary places to adjust the local PTS to global
             * PTS concerning the whole stream. */
            List<SegmentIndexBox> segmentIndexBoxes = fragment.getBoxes(SegmentIndexBox.class);
            if(segmentIndexBoxes.size() > 0) {
                SegmentIndexBox sidx = segmentIndexBoxes.get(0);
                segmentPTSOffsetUs = (long) ((double) sidx.getEarliestPresentationTime() / sidx.getTimeScale() * 1000000);
            }
            /* If there is no segment index box to read the PTS from, we calculate the PTS offset
             * from the info given in the MPD. */
            else {
                segmentPTSOffsetUs = cachedSegment.number * cachedSegment.representation.segmentDurationUs;
            }

            Movie mp4Segment = new Movie();
            for(TrackBox trackBox : baseIsoFile.getMovieBox().getBoxes(TrackBox.class)) {
                mp4Segment.addTrack(new Mp4TrackImpl(null, trackBox, fragment));
            }
            Container mp4SegmentContainer = mMp4Builder.build(mp4Segment);
            FileOutputStream fos = new FileOutputStream(segmentFile, false);
            mp4SegmentContainer.writeContainer(fos.getChannel());
            fos.close();
        } else {
            // merge init and media segments into file
            BufferedSink segmentFileSink = Okio.buffer(Okio.sink(segmentFile));
            segmentFileSink.write(mInitSegments.get(cachedSegment.representation));
            segmentFileSink.write(mediaSegment);
            segmentFileSink.close();
        }

        cachedSegment.file = segmentFile;
        cachedSegment.ptsOffsetUs = segmentPTSOffsetUs;
    }

    private class SegmentDownloadCallback implements Callback {

        private CachedSegment mCachedSegment;

        private SegmentDownloadCallback(CachedSegment cachedSegment) {
            mCachedSegment = cachedSegment;
        }

        @Override
        public void onFailure(Request request, IOException e) {
            if(mFutureCacheRequests.remove(mCachedSegment) != null) {
                Log.e(TAG, "onFailure", e);
            } else {
                // If a call is not in the requests map anymore, it has been cancelled and didn't really fail
            }
        }

        @Override
        public void onResponse(Response response) throws IOException {
            if(response.isSuccessful()) {
                try {
                    long startTime = SystemClock.elapsedRealtime();
                    byte[] segmentData = response.body().bytes();

                    /* The time it takes to send the request header to the server until the response
                     * headers arrive. Can be custom implemented through an Interceptor too, in case
                     * this should ever fail in the future. */
                    long headerTime = Long.parseLong(response.header(OkHeaders.RECEIVED_MILLIS)) - Long.parseLong(response.header(OkHeaders.SENT_MILLIS));

                    /* The time it takes to read the result body, which is the actual segment data.
                     * The sum of this time together with the header time is the total segment download time. */
                    long payloadTime = SystemClock.elapsedRealtime() - startTime;

                    mAdaptationLogic.reportSegmentDownload(mAdaptationSet, mCachedSegment.representation, mCachedSegment.segment, segmentData.length, headerTime + payloadTime);
                    handleSegment(segmentData, mCachedSegment);
                    mFutureCacheRequests.remove(mCachedSegment.number);
                    mFutureCache.put(mCachedSegment.number, mCachedSegment);
                    Log.d(TAG, "async cached " + mCachedSegment.number + " " + mCachedSegment.segment.toString() + " -> " + mCachedSegment.file.getPath());
                    synchronized (mFutureCache) {
                        mFutureCache.notify();
                    }
                } catch(Exception e) {
                    Log.e(TAG, "onResponse", e);
                } finally {
                    response.body().close();
                }
            }
        }
    }
}
