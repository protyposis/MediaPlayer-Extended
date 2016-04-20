/*
 * Copyright (c) 2016 Mario Guggenberger <mg@protyposis.net>
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
    }

    interface OnDecoderEventListener {
        void onBuffering(MediaCodecDecoder decoder);
    }

    protected String TAG = MediaCodecDecoder.class.getSimpleName();

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
    }

    protected MediaFormat getFormat() {
        return mFormat;
    }

    protected MediaCodec getCodec() {
        return mCodec;
    }

    protected boolean isInputEos() {
        return mInputEos;
    }

    protected boolean isOutputEos() {
        return mOutputEos;
    }

    /**
     * Restarts the codec with a new format, e.g. after a representation change.
     */
    private void reinitCodec() {
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
        for(int i = 0; i < mCodecOutputBuffers.length; i++) {
            mEmptyFrameInfos.add(new FrameInfo());
        }

        Log.d(TAG, "reinitCodec " + (SystemClock.elapsedRealtime() - t1) + "ms");
    }

    protected void configureCodec(MediaCodec codec, MediaFormat format) {
        codec.configure(format, null, null, 0);
    }

    protected void reconfigureCodec() {
        reinitCodec();
    }

    /**
     * Skips to the next sample of this decoder's track by skipping all samples belonging to other decoders.
     */
    public void skipToNextSample() {
        if(mPassive) return;

        int trackIndex;
        while ((trackIndex = mExtractor.getSampleTrackIndex()) != -1 && trackIndex != mTrackIndex && !mInputEos) {
            mExtractor.advance();
        }
    }

    public boolean queueSampleToCodec(boolean skip) {
        if(mInputEos) return false;

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

                if(mExtractor.getCachedDuration() == 0) {
                    if(mOnDecoderEventListener != null) {
                        mOnDecoderEventListener.onBuffering(this);
                    }
                }
            } else {
                int sampleSize = mExtractor.readSampleData(inputBuffer, 0);
                long presentationTimeUs = 0;

                if (sampleSize < 0) {
                    Log.d(TAG, "EOS video input");
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

                if (!mInputEos) {
                    mExtractor.advance();
                }
            }
        }
        return sampleQueued;
    }

    public FrameInfo dequeueDecodedFrame() {
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

            //Log.d(TAG, "PTS " + vfi.presentationTimeUs);

            if(fi.endOfStream) Log.d(TAG, "EOS output");

            return fi;
        } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            mCodecOutputBuffers = mCodec.getOutputBuffers();
            Log.d(TAG, "output buffers have changed.");
        } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            // NOTE: this is the format of the raw output, not the format as specified by the container
            MediaFormat oformat = mCodec.getOutputFormat();
            Log.d(TAG, "output format has changed to " + oformat);
        } else if (res == MediaCodec.INFO_TRY_AGAIN_LATER) {
            //Log.d(TAG, "dequeueOutputBuffer timed out");
        }

        //Log.d(TAG, "EOS NULL");
        return null; // EOS already reached, no frame left to return
    }

    public void releaseFrame(FrameInfo frameInfo) {
        mCodec.releaseOutputBuffer(frameInfo.buffer, false);
        releaseFrameInfo(frameInfo);
    }

    protected void releaseFrameInfo(FrameInfo frameInfo) {
        frameInfo.clear();
        mEmptyFrameInfos.add(frameInfo);
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
    public FrameInfo decodeFrame(boolean skip, boolean force) {
        //Log.d(TAG, "decodeFrame");
        while(!mOutputEos) {
            // Dequeue decoded frames
            FrameInfo frameInfo = dequeueDecodedFrame();

            // Enqueue encoded buffers into decoders
            while (!mRepresentationChanging
                    && !mInputEos
                    && queueSampleToCodec(skip)) {}

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

    public FrameInfo seekTo(MediaPlayer.SeekMode seekMode, long seekTargetTimeUs) throws IOException {
        return seekTo(seekMode, seekTargetTimeUs, mExtractor, mCodec);
    }

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

        extractor.seekTo(seekTargetTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

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
     * Releases the codec. This must be called to free decoder resources when this object is no longer in use.
     */
    public void release() {
        mCodec.stop();
        mCodec.release();
        Log.d(TAG, "decoder released");
    }
}
