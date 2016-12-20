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

package net.protyposis.android.mediaplayer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mario on 13.09.2015.
 */
abstract class MediaCodecDecoder {

    static class FrameInfo {
        int buffer;
        ByteBuffer data;
        long presentationTimeUs;
        boolean endOfStream;
        boolean representationChanged;

        public FrameInfo() {
            clear();
        }

        public void clear() {
            buffer = -1;
            data = null;
            presentationTimeUs = -1;
            endOfStream = false;
            representationChanged = false;
        }

        @Override
        public String toString() {
            return "FrameInfo{" +
                    "buffer=" + buffer +
                    ", data=" + data +
                    ", presentationTimeUs=" + presentationTimeUs +
                    ", endOfStream=" + endOfStream +
                    ", representationChanged=" + representationChanged +
                    '}';
        }
    }

    interface OnDecoderEventListener {
        void onBuffering(MediaCodecDecoder decoder);
    }

    protected String TAG = MediaCodecDecoder.class.getSimpleName();

    public static final long PTS_NONE = Long.MIN_VALUE;
    public static final long PTS_EOS = Long.MAX_VALUE;

    private static final long TIMEOUT_US = 0;
    public static final int INDEX_NONE = -1;

    private MediaExtractor mExtractor;
    private int mTrackIndex;
    private MediaFormat mFormat;
    private MediaCodec mCodec;
    private ByteBuffer[] mCodecInputBuffers;
    private ByteBuffer[] mCodecOutputBuffers;
    private MediaCodec.BufferInfo mBufferInfo;
    private boolean mInputEos;
    private boolean mOutputEos;
    private List<FrameInfo> mEmptyFrameInfos;

    /* Flag notifying that the representation has changed in the extractor and needs to be passed
     * to the decoder. This transition state is only needed in playback, not when seeking. */
    private boolean mRepresentationChanging;
    /* Flag notifying that the decoder has changed to a new representation, post-actions need to
     * be carried out. */
    private boolean mRepresentationChanged;

    private OnDecoderEventListener mOnDecoderEventListener;

    /**
     * Flag for passive mode. When a decoder is in passive mode, it does not actively control
     * the extractor, because the extractor is controlled from another decoder instance. It does
     * therefore also not execute any operations that affect the extractor in any way (e.g. seeking).
     *
     */
    private boolean mPassive;

    private long mDecodingPTS;

    private FrameInfo mCurrentFrameInfo;

    public MediaCodecDecoder(MediaExtractor extractor, boolean passive, int trackIndex,
                             OnDecoderEventListener listener)
            throws IllegalStateException, IOException
    {
        // Apply the name of the concrete class that extends this base class to the logging tag
        // THis is really not a nice solution but there's no better one: http://stackoverflow.com/a/936724
        TAG = getClass().getSimpleName();

        if(extractor == null || trackIndex == INDEX_NONE) {
            throw new IllegalArgumentException("no track specified");
        }

        mExtractor = extractor;
        mPassive = passive;
        mTrackIndex = trackIndex;
        mFormat = extractor.getTrackFormat(mTrackIndex);

        mOnDecoderEventListener = listener;

        mCodec = MediaCodec.createDecoderByType(mFormat.getString(MediaFormat.KEY_MIME));

        mDecodingPTS = PTS_NONE;
    }

    protected final MediaFormat getFormat() {
        return mFormat;
    }

    protected final MediaCodec getCodec() {
        return mCodec;
    }

    protected final boolean isInputEos() {
        return mInputEos;
    }

    protected final boolean isOutputEos() {
        return mOutputEos;
    }

    protected final boolean isPassive() {
        return mPassive;
    }

    /**
     * Starts or restarts the codec with a new format, e.g. after a representation change.
     */
    protected final void reinitCodec() {
        try {
            long t1 = SystemClock.elapsedRealtime();

            // Get new format and restart codec with this format
            mFormat = mExtractor.getTrackFormat(mTrackIndex);

            mCodec.stop();
            configureCodec(mCodec, mFormat);
            mCodec.start(); // TODO speedup, but how? this takes a long time and introduces lags when switching DASH representations (AVC codec)
            mCodecInputBuffers = mCodec.getInputBuffers();
            mCodecOutputBuffers = mCodec.getOutputBuffers();
            mBufferInfo = new MediaCodec.BufferInfo();
            mInputEos = false;
            mOutputEos = false;

            // Create FrameInfo objects for later reuse
            mEmptyFrameInfos = new ArrayList<>();
            for (int i = 0; i < mCodecOutputBuffers.length; i++) {
                mEmptyFrameInfos.add(new FrameInfo());
            }

            Log.d(TAG, "reinitCodec " + (SystemClock.elapsedRealtime() - t1) + "ms");
        } catch (IllegalArgumentException e) {
            mCodec.release(); // Release failed codec to not leak a codec thread (MediaCodec_looper)
            Log.e(TAG, "reinitCodec: invalid surface or format");
            throw e;
        } catch (IllegalStateException e) {
            mCodec.release(); // Release failed codec to not leak a codec thread (MediaCodec_looper)
            Log.e(TAG, "reinitCodec: illegal state");
            throw e;
        }
    }

    /**
     * Configures the codec during initialization. Should be overwritten by subclasses that require
     * a more specific configuration.
     *
     * @param codec the codec to configure
     * @param format the format to configure the codec with
     */
    protected void configureCodec(MediaCodec codec, MediaFormat format) {
        codec.configure(format, null, null, 0);
    }

    /**
     * Skips to the next sample of this decoder's track by skipping all samples belonging to other decoders.
     */
    public final void skipToNextSample() {
        if(mPassive) return;

        int trackIndex;
        while ((trackIndex = mExtractor.getSampleTrackIndex()) != -1 && trackIndex != mTrackIndex && !mInputEos) {
            mExtractor.advance();
        }
    }

    /**
     * Checks any constraints if it is a good idea to decode another frame. Returns true by default,
     * and is meant to be overwritten by subclasses with special behavior, e.g. an audio track might
     * limit filling of the playback buffer.
     *
     * @return value telling if another frame should be decoded
     */
    protected boolean shouldDecodeAnotherFrame() {
        return true;
    }

    /**
     * Queues a sample from the MediaExtractor to the input of the MediaCodec. The return value
     * signals if the operation was successful and can be tried another time (return true), or if
     * there are no more input buffers available, the next sample does not belong to this decoder
     * (if skip is false) or the input EOS is reached (return false).
     *
     * @param skip if true, samples belonging to foreign tracks are skipped
     * @return true if the operation can be repeated for another sample, false if it's another
     * decoder's turn or the EOS
     */
    public final boolean queueSampleToCodec(boolean skip) {
        if(mInputEos || !shouldDecodeAnotherFrame()) return false;

        // If we are not at the EOS and the current extractor track is not the this track, we
        // return false because it is some other decoder's turn now.
        // If we are at the EOS, the following code will issue a BUFFER_FLAG_END_OF_STREAM.
        if(mExtractor.getSampleTrackIndex() != -1 && mExtractor.getSampleTrackIndex() != mTrackIndex) {
            if(skip) return mExtractor.advance();
            return false;
        }

        boolean sampleQueued = false;
        int inputBufIndex = mCodec.dequeueInputBuffer(TIMEOUT_US);
        if (inputBufIndex >= 0) {
            ByteBuffer inputBuffer = mCodecInputBuffers[inputBufIndex];

            if(mExtractor.hasTrackFormatChanged()) {
                /* The mRepresentationChanging flag and BUFFER_FLAG_END_OF_STREAM flag together
                 * notify the decoding loop that the representation changes and the codec
                 * needs to be reconfigured.
                 */
                mRepresentationChanging = true;
                mCodec.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                // Check buffering state before representation changes (and possibly a new segment needs to be downloaded)
                if(mExtractor.getCachedDuration() > -1) {
                    if(mOnDecoderEventListener != null) {
                        mOnDecoderEventListener.onBuffering(this);
                    }
                }
            } else {
                // Check buffering state before the blocking readSampleData call
                if(mExtractor.getCachedDuration() > -1) {
                    if(mOnDecoderEventListener != null) {
                        mOnDecoderEventListener.onBuffering(this);
                    }
                }
                int sampleSize = mExtractor.readSampleData(inputBuffer, 0);
                long presentationTimeUs = 0;

                if (sampleSize < 0) {
                    Log.d(TAG, "EOS input");
                    mInputEos = true;
                    sampleSize = 0;
                } else {
                    presentationTimeUs = mExtractor.getSampleTime();
                    sampleQueued = true;
                }

                mCodec.queueInputBuffer(
                        inputBufIndex,
                        0,
                        sampleSize,
                        presentationTimeUs,
                        mInputEos ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                //Log.d(TAG, "queued PTS " + presentationTimeUs);

                if (!mInputEos) {
                    mExtractor.advance();
                }
            }
        }
        return sampleQueued;
    }

    /**
     * Consumes a decoded frame from the decoder output and returns information about it.
     *
     * @return a FrameInfo if a frame was available; NULL if the decoder needs more input
     * samples/decoding time or if the output EOS has been reached
     */
    public final FrameInfo dequeueDecodedFrame() {
        if(mOutputEos) return null;

        int res = mCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
        mOutputEos = res >= 0 && (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;

        if(mOutputEos && mRepresentationChanging) {
            /* Here, the output is not really at its end, it's just the end of the
             * current representation segment, and the codec needs to be reconfigured to
             * the following representation format to carry on.
             */

            reinitCodec();

            mOutputEos = false;
            mRepresentationChanging = false;
            mRepresentationChanged = true;
        }
        else if (res >= 0) {
            // Frame decoded. Fill frame info object and return to caller...

            // Adjust buffer: http://bigflake.com/mediacodec/#q11
            // This is done on audio buffers only, video decoder does not return actual buffers
            ByteBuffer data = mCodecOutputBuffers[res];
            if (data != null && mBufferInfo.size != 0) {
                data.position(mBufferInfo.offset);
                data.limit(mBufferInfo.offset + mBufferInfo.size);
                //Log.d(TAG, "raw data bytes: " + mBufferInfo.size);
            }

            FrameInfo fi = mEmptyFrameInfos.get(0);
            fi.buffer = res;
            fi.data = data;
            fi.presentationTimeUs = mBufferInfo.presentationTimeUs;
            fi.endOfStream = mOutputEos;

            if(mRepresentationChanged) {
                mRepresentationChanged = false;
                fi.representationChanged = true;
            }
            if(fi.endOfStream) {
                Log.d(TAG, "EOS output");
            } else {
                mDecodingPTS = fi.presentationTimeUs;
            }

            //Log.d(TAG, "decoded PTS " + fi.presentationTimeUs);

            return fi;
        } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            mCodecOutputBuffers = mCodec.getOutputBuffers();
            Log.d(TAG, "output buffers have changed.");
        } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            // NOTE: this is the format of the raw output, not the format as specified by the container
            MediaFormat format = mCodec.getOutputFormat();
            Log.d(TAG, "output format has changed to " + format);
            onOutputFormatChanged(format);
        } else if (res == MediaCodec.INFO_TRY_AGAIN_LATER) {
            //Log.d(TAG, "dequeueOutputBuffer timed out");
        }

        //Log.d(TAG, "EOS NULL");
        return null; // EOS already reached, no frame left to return
    }

    /**
     * Returns the PTS of the current, that is, the most recently decoded frame.
     * @return the PTS of the most recent frame
     */
    public long getCurrentDecodingPTS() {
        return mDecodingPTS;
    }

    /**
     * Returns the duration if the cached data in the extractor, or -1 if the extractor does not
     * support or does not need caching (e.g. local files).
     * @return the duration of the cached data or -1 if caching is not active
     */
    public long getCachedDuration() {
        return mExtractor.getCachedDuration();
    }

    /**
     * Returns true iff we are caching data and the cache has reached the
     * end of the data stream.
     * @see MediaExtractor#hasCacheReachedEndOfStream()
     * @return true if caching and end of stream has been reached, else false
     */
    public boolean hasCacheReachedEndOfStream() {
        return mExtractor.hasCacheReachedEndOfStream();
    }

    /**
     * Renders a frame at the specified offset time to some output (e.g. video frame to screen,
     * audio frame to audio track).
     * @param frameInfo the frame info holding the frame buffer
     * @param offsetUs the offset from now when the frame should be rendered
     */
    public void renderFrame(FrameInfo frameInfo, long offsetUs) {
        releaseFrame(frameInfo);
    }

    /**
     * Renders the current frame instantly.
     * This only works if the decoder holds a current frame, e.g. after a seek.
     * @see #renderFrame(FrameInfo, long)
     */
    public void renderFrame() {
        if(mCurrentFrameInfo != null) renderFrame(mCurrentFrameInfo, 0);
    }

    /**
     * Dismisses a frame without rendering it.
     * @param frameInfo the frame info holding the frame buffer to dismiss
     */
    public void dismissFrame(FrameInfo frameInfo) {
        releaseFrame(frameInfo);
    }

    /**
     * Dismisses the current frame.
     * This only works if the decoder holds a current frame, e.g. after a seek.
     */
    public void dismissFrame() {
        if(mCurrentFrameInfo != null) dismissFrame(mCurrentFrameInfo);
    }

    /**
     * Releases a frame and all its associated resources.
     * When overwritten, this method must release the output buffer through
     * {@link MediaCodec#releaseOutputBuffer(int, boolean)} or {@link MediaCodec#releaseOutputBuffer(int, long)},
     * and then release the frame info through {@link #releaseFrameInfo(FrameInfo)}.
     *
     * @param frameInfo information about the current frame
     */
    public void releaseFrame(FrameInfo frameInfo) {
        mCodec.releaseOutputBuffer(frameInfo.buffer, false);
        releaseFrameInfo(frameInfo);
    }

    /**
     * Releases the frame info back into the decoder for later reuse. This method must always be
     * called after handling a frame.
     *
     * @param frameInfo information about a frame
     */
    protected final void releaseFrameInfo(FrameInfo frameInfo) {
        frameInfo.clear();
        mEmptyFrameInfos.add(frameInfo);
    }

    /**
     * Overwrite in subclass to handle a change of the output format.
     * @param format the new media format
     */
    protected void onOutputFormatChanged(MediaFormat format) {
        // nothing to do here
    }

    /**
     * Runs the decoder loop, optionally until a new frame is available.
     * The returned FrameInfo object keeps metadata of the decoded frame. To release its data,
     * call {@link #releaseFrame(FrameInfo)}.
     *
     * @param skip skip frames of other tracks
     * @param force force decoding in a loop until a frame becomes available or the EOS is reached
     * @return a FrameInfo object holding metadata of a decoded frame or NULL if no frame has been decoded
     */
    public final FrameInfo decodeFrame(boolean skip, boolean force) {
        //Log.d(TAG, "decodeFrame");
        while(!mOutputEos) {
            // Dequeue decoded frames
            FrameInfo frameInfo = dequeueDecodedFrame();

            // Enqueue encoded buffers into decoders
            while (queueSampleToCodec(skip)) {}

            if(frameInfo != null) {
                // If a frame has been decoded, return it
                return frameInfo;
            }

            if(!force) {
                // If we have not decoded a frame and we're not forcing decoding until a frame becomes available, return null
                return null;
            }
        }

        Log.d(TAG, "EOS NULL");
        return null; // EOS already reached, no frame left to return
    }

    /**
     * Seeks to the specified target PTS with the specified seek mode. After the seek, the decoder
     * holds the frame from the target position which must either be rendered through {@link #renderFrame()}
     * or dismissed through {@link #dismissFrame()}.
     *
     * @param seekMode the mode how the seek should be carried out
     * @param seekTargetTimeUs the target PTS to seek to
     * @throws IOException
     */
    public final void seekTo(MediaPlayer.SeekMode seekMode, long seekTargetTimeUs) throws IOException {
        mDecodingPTS = PTS_NONE;
        mCurrentFrameInfo = seekTo(seekMode, seekTargetTimeUs, mExtractor, mCodec);
    }

    /**
     * This method implements the actual seeking and can be overwritten by subclasses to implement
     * custom seeking methods.
     *
     * @see #seekTo(MediaPlayer.SeekMode, long)
     */
    protected FrameInfo seekTo(MediaPlayer.SeekMode seekMode, long seekTargetTimeUs,
                               MediaExtractor extractor, MediaCodec codec) throws IOException {
        if(mPassive) {
            // Even when not actively seeking, the codec must be flushed to get rid of left over
            // audio frames from the previous playback position and the EOS flags need to be reset too.
            mInputEos = false;
            mOutputEos = false;
            codec.flush();
            return null;
        }

        Log.d(TAG, "seeking to:                 " + seekTargetTimeUs);
        Log.d(TAG, "extractor current position: " + extractor.getSampleTime());

        extractor.seekTo(seekTargetTimeUs, seekMode.getBaseSeekMode());

        Log.d(TAG, "extractor new position:     " + extractor.getSampleTime());

        // TODO add seek cancellation possibility
        // e.g. by returning an object with a cancel method and checking the flag at fitting places within this method

        mInputEos = false;
        mOutputEos = false;
        codec.flush();

        if(extractor.hasTrackFormatChanged()) {
            reinitCodec();
            mRepresentationChanged = true;
        }

        return decodeFrame(true, true);
    }

    /**
     * Releases the codec and its resources. Must be called when the decoder is no longer in use.
     */
    public void release() {
        mCodec.stop();
        mCodec.release();
        Log.d(TAG, "decoder released");
    }
}
