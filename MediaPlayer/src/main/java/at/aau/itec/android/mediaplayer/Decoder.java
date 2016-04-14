/*
 * Copyright (c) 2015 Mario Guggenberger <mg@protyposis.net>
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

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mario on 13.09.2015.
 */
class Decoder {

    static class VideoFrameInfo {
        int buffer;
        long presentationTimeUs;
        boolean endOfStream;
        boolean representationChanged;
        int width;
        int height;

        public VideoFrameInfo() {
            clear();
        }

        public void clear() {
            buffer = -1;
            presentationTimeUs = -1;
            endOfStream = false;
            representationChanged = false;
            width = -1;
            height = -1;
        }
    }

    interface OnDecoderEventListener {
        void onBuffering(Decoder decoder);
    }

    private static final String TAG = Decoder.class.getSimpleName();

    private static final long TIMEOUT_US = 0;
    public static final int INDEX_NONE = -1;

    private MediaExtractor mVideoExtractor;
    private int mVideoTrackIndex;
    private Surface mVideoSurface;
    private MediaFormat mVideoFormat;

    private MediaExtractor mAudioExtractor;
    private int mAudioTrackIndex;
    private AudioPlayback mAudioPlayback;
    private MediaFormat mAudioFormat;

    private MediaCodec mVideoCodec;
    private MediaCodec mAudioCodec;

    private ByteBuffer[] mVideoCodecInputBuffers;
    private ByteBuffer[] mVideoCodecOutputBuffers;
    private ByteBuffer[] mAudioCodecInputBuffers;
    private ByteBuffer[] mAudioCodecOutputBuffers;
    private MediaCodec.BufferInfo mVideoInfo;
    private MediaCodec.BufferInfo mAudioInfo;
    private boolean mVideoInputEos;
    private boolean mVideoOutputEos;
    private boolean mAudioInputEos;
    private boolean mAudioOutputEos;
    private List<VideoFrameInfo> mEmptyVideoFrameInfos;

    /* Flag notifying that the representation has changed in the extractor and needs to be passed
     * to the decoder. This transition state is only needed in playback, not when seeking. */
    private boolean mRepresentationChanging;
    /* Flag notifying that the decoder has changed to a new representation, post-actions need to
     * be carried out. */
    private boolean mRepresentationChanged;

    private OnDecoderEventListener mOnDecoderEventListener;

    public Decoder(MediaExtractor videoExtractor, int videoTrackIndex, Surface videoSurface,
                   MediaExtractor audioExtractor, int audioTrackIndex, AudioPlayback audioPlayback,
                   OnDecoderEventListener listener)
            throws IllegalStateException, IOException
    {
        if(videoExtractor == null || videoTrackIndex == INDEX_NONE) {
            if(audioTrackIndex != INDEX_NONE) {
                throw new IllegalArgumentException("audio-only not supported yet");
            }
            throw new IllegalArgumentException("no video track specified");
        }

        mVideoExtractor = videoExtractor;
        mVideoTrackIndex = videoTrackIndex;
        mVideoSurface = videoSurface;
        mVideoFormat = videoExtractor.getTrackFormat(mVideoTrackIndex);

        mAudioExtractor = audioExtractor;
        mAudioTrackIndex = audioTrackIndex;
        mAudioPlayback = audioPlayback;

        mOnDecoderEventListener = listener;

        if(audioTrackIndex != INDEX_NONE) {
            if(mAudioExtractor == null) {
                mAudioExtractor = mVideoExtractor;
            }
            if(mAudioPlayback == null) {
                throw new IllegalArgumentException("audio playback missing");
            }
            mAudioFormat = mAudioExtractor.getTrackFormat(mAudioTrackIndex);
        }

        mVideoCodec = MediaCodec.createDecoderByType(mVideoFormat.getString(MediaFormat.KEY_MIME));

        if(mAudioFormat != null) {
            mAudioCodec = MediaCodec.createDecoderByType(mAudioFormat.getString(MediaFormat.KEY_MIME));
        }

        reinitCodecs();

        // Create VideoFrameInfo objects for later reuse
        mEmptyVideoFrameInfos = new ArrayList<>();
        for(int i = 0; i < mVideoCodecOutputBuffers.length; i++) {
            mEmptyVideoFrameInfos.add(new VideoFrameInfo());
        }
    }

    /**
     * Restarts the codecs with a new format, e.g. after a representation change.
     */
    private void reinitCodecs() {
        long t1 = SystemClock.elapsedRealtime();

        // Get new video format and restart video codec with this format
        mVideoFormat = mVideoExtractor.getTrackFormat(mVideoTrackIndex);

        mVideoCodec.stop();
        mVideoCodec.configure(mVideoFormat, mVideoSurface, null, 0);
        mVideoCodec.start();
        mVideoCodecInputBuffers = mVideoCodec.getInputBuffers();
        mVideoCodecOutputBuffers = mVideoCodec.getOutputBuffers();
        mVideoInfo = new MediaCodec.BufferInfo();
        mVideoInputEos = false;
        mVideoOutputEos = false;

        if(mAudioFormat != null) {
            mAudioFormat = mAudioExtractor.getTrackFormat(mAudioTrackIndex);

            mAudioCodec.stop();
            mAudioCodec.configure(mAudioFormat, null, null, 0);
            mAudioCodec.start();
            mAudioCodecInputBuffers = mAudioCodec.getInputBuffers();
            mAudioCodecOutputBuffers = mAudioCodec.getOutputBuffers();
            mAudioInfo = new MediaCodec.BufferInfo();
            mAudioInputEos = false;
            mAudioOutputEos = false;

            mAudioPlayback.init(mAudioFormat);
        }

        Log.d(TAG, "reinitCodecs " + (SystemClock.elapsedRealtime() - t1) + "ms");
    }

    private boolean queueVideoSampleToCodec() {
        /* NOTE the track index checks only for debugging
         * when enabled, they prevent the EOS detection and handling below */
//        int trackIndex = mVideoExtractor.getSampleTrackIndex();
//        if(trackIndex == -1) {
//            throw new IllegalStateException("EOS");
//        }
//        if(trackIndex != mVideoTrackIndex) {
//            throw new IllegalStateException("wrong track index: " + trackIndex);
//        }

        // If we are not at the EOS and the current extractor track is not the video track, we
        // return false because it is some other decoder's turn now (e.g. audio).
        // If we are at the EOS, the next block will issue a BUFFER_FLAG_END_OF_STREAM.
        if(mVideoExtractor.getSampleTrackIndex() != -1 && mVideoExtractor.getSampleTrackIndex() != mVideoTrackIndex) {
            return false;
        }

        boolean sampleQueued = false;
        int inputBufIndex = mVideoCodec.dequeueInputBuffer(TIMEOUT_US);
        if (inputBufIndex >= 0) {
            ByteBuffer inputBuffer = mVideoCodecInputBuffers[inputBufIndex];
            int sampleSize = mVideoExtractor.readSampleData(inputBuffer, 0);
            long presentationTimeUs = 0;

            if(sampleSize == 0) {
                if(mVideoExtractor.getCachedDuration() == 0) {
                    if(mOnDecoderEventListener != null) {
                        mOnDecoderEventListener.onBuffering(this);
                    }
                }
                if(mVideoExtractor.hasTrackFormatChanged()) {
                    /* The mRepresentationChanging flag and BUFFER_FLAG_END_OF_STREAM flag together
                     * notify the decoding loop that the representation changes and the codec
                     * needs to be reconfigured.
                     */
                    mRepresentationChanging = true;
                    mVideoCodec.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
            } else {
                if (sampleSize < 0) {
                    Log.d(TAG, "EOS video input");
                    mVideoInputEos = true;
                    sampleSize = 0;
                } else {
                    presentationTimeUs = mVideoExtractor.getSampleTime();
                    sampleQueued = true;
                }

                mVideoCodec.queueInputBuffer(
                        inputBufIndex,
                        0,
                        sampleSize,
                        presentationTimeUs,
                        mVideoInputEos ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                if (!mVideoInputEos) {
                    mVideoExtractor.advance();
                }
            }
        }
        return sampleQueued;
    }

    private boolean queueAudioSampleToCodec(MediaExtractor extractor, boolean skip) {
        if(mAudioCodec == null) {
            throw new IllegalStateException("no audio track configured");
        }
        /* NOTE the track index checks only for debugging
         * when enabled, they prevent the EOS detection and handling below */
//        int trackIndex = extractor.getSampleTrackIndex();
//        if(trackIndex == -1) {
//            throw new IllegalStateException("EOS");
//        }
//        if(trackIndex != mAudioTrackIndex) {
//            throw new IllegalStateException("wrong track index: " + trackIndex);
//        }

        // If we are not at the EOS and the current extractor track is not the audio track, we
        // return false because it is some other decoder's turn now (e.g. video).
        // If we are at the EOS, the next block will issue a BUFFER_FLAG_END_OF_STREAM.
        if(extractor.getSampleTrackIndex() != -1 && extractor.getSampleTrackIndex() != mAudioTrackIndex) {
            return false;
        }

        if(skip && !mAudioInputEos) {
            if(mAudioExtractor == mVideoExtractor) {
                // If audio and video are muxed, skipping the audio means skipping all audio frames,
                // so we advance over the audio frame and return true so that this method is called
                // again and skips another audio frame, etc.
                extractor.advance();
                return true;
            } else {
                // If the audio stream is separate, skipping the audio just means to not process the
                // audio stream.
                return false;
            }
        }
        boolean sampleQueued = false;
        int inputBufIndex = mAudioCodec.dequeueInputBuffer(TIMEOUT_US);
        if (inputBufIndex >= 0) {
            ByteBuffer inputBuffer = mAudioCodecInputBuffers[inputBufIndex];
            int sampleSize = extractor.readSampleData(inputBuffer, 0);
            long presentationTimeUs = 0;
            if (sampleSize < 0) {
                Log.d(TAG, "EOS audio input");
                mAudioInputEos = true;
                sampleSize = 0;
            } else if(sampleSize == 0) {
                if(extractor.getCachedDuration() == 0) {
                    if(mOnDecoderEventListener != null) {
                        mOnDecoderEventListener.onBuffering(this);
                    }
                }
            } else {
                presentationTimeUs = extractor.getSampleTime();
                sampleQueued = true;
            }
            mAudioCodec.queueInputBuffer(
                    inputBufIndex,
                    0,
                    sampleSize,
                    presentationTimeUs,
                    mAudioInputEos ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

            if (!mAudioInputEos) {
                extractor.advance();
            }
        }
        return sampleQueued;
    }

    private boolean queueMediaSampleToCodec(boolean videoOnly) {
        boolean result = false;
        if(mAudioCodec != null) {
            if (videoOnly) {
                /* VideoOnly mode skips audio samples, e.g. while doing a seek operation. */
                int trackIndex;
                while ((trackIndex = mVideoExtractor.getSampleTrackIndex()) != -1 && trackIndex != mVideoTrackIndex && !mVideoInputEos) {
                    mVideoExtractor.advance();
                }
            } else {
                while (mVideoExtractor == mAudioExtractor && mAudioExtractor.getSampleTrackIndex() == mAudioTrackIndex) {
                    result = queueAudioSampleToCodec(mAudioExtractor, false);
                    dequeueDecodedAudioFrame();
                }
            }
        }
        if(!mVideoInputEos) {
            result = queueVideoSampleToCodec();
        }
        return result;
    }

    private VideoFrameInfo dequeueDecodedVideoFrame() {
        int res = mVideoCodec.dequeueOutputBuffer(mVideoInfo, TIMEOUT_US);
        mVideoOutputEos = res >= 0 && (mVideoInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;

        if(mVideoOutputEos && mRepresentationChanging) {
            /* Here, the video output is not really at its end, it's just the end of the
             * current representation segment, and the codec needs to be reconfigured to
             * the following representation format to carry on.
             */

            reinitCodecs();

            mVideoOutputEos = false;
            mRepresentationChanging = false;
            mRepresentationChanged = true;
        }
        else if (res >= 0) {
            // Frame decoded. Fill video frame info object and return to caller...
            //Log.d(TAG, "pts=" + info.presentationTimeUs);

            VideoFrameInfo vfi = mEmptyVideoFrameInfos.get(0);
            vfi.buffer = res;
            vfi.presentationTimeUs = mVideoInfo.presentationTimeUs;
            vfi.endOfStream = mVideoOutputEos;

            if(mRepresentationChanged) {
                mRepresentationChanged = false;
                vfi.representationChanged = true;
                vfi.width = getVideoWidth();
                vfi.height = getVideoHeight();
            }

            //Log.d(TAG, "PTS " + vfi.presentationTimeUs);

            if(vfi.endOfStream) Log.d(TAG, "EOS");

            return vfi;
        } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            mVideoCodecOutputBuffers = mVideoCodec.getOutputBuffers();
            Log.d(TAG, "output buffers have changed.");
        } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            // NOTE: this is the format of the raw video output, not the video format as specified by the container
            MediaFormat oformat = mVideoCodec.getOutputFormat();
            Log.d(TAG, "output format has changed to " + oformat);
        } else if (res == MediaCodec.INFO_TRY_AGAIN_LATER) {
            //Log.d(TAG, "dequeueOutputBuffer timed out");
        }

        //Log.d(TAG, "EOS NULL");
        return null; // EOS already reached, no video frame left to return
    }

    private boolean dequeueDecodedAudioFrame() {
        boolean frameDecoded = false;

        int output = mAudioCodec.dequeueOutputBuffer(mAudioInfo, TIMEOUT_US);
        if (output >= 0) {
            // http://bigflake.com/mediacodec/#q11
            ByteBuffer outputData = mAudioCodecOutputBuffers[output];
            if (mAudioInfo.size != 0) {
                outputData.position(mAudioInfo.offset);
                outputData.limit(mAudioInfo.offset + mAudioInfo.size);
                //Log.d(TAG, "raw audio data bytes: " + mVideoInfo.size);
            }
            mAudioPlayback.write(outputData, mAudioInfo.presentationTimeUs);
            mAudioCodec.releaseOutputBuffer(output, false);
            frameDecoded = true;

            if ((mAudioInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                mAudioOutputEos = true;
                Log.d(TAG, "EOS audio output");
            }
        } else if (output == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            Log.d(TAG, "audio output buffers have changed.");
            mAudioCodecOutputBuffers = mAudioCodec.getOutputBuffers();
            frameDecoded = true;
        } else if (output == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat format = mAudioCodec.getOutputFormat();
            Log.d(TAG, "audio output format has changed to " + format);
            frameDecoded = true;
            mAudioPlayback.init(format);
        } else if (output == MediaCodec.INFO_TRY_AGAIN_LATER) {
            //Log.d(TAG, "audio dequeueOutputBuffer timed out");
        }

        return frameDecoded;
    }

    /**
     * Runs the decoder until a new frame is available. The returned VideoFrameInfo object keeps
     * metadata of the decoded frame. To render the frame to the screen and/or dismiss its data,
     * call {@link #releaseFrame(VideoFrameInfo, boolean)}.
     */
    public VideoFrameInfo decodeFrame(boolean videoOnly) {
        while(!mVideoOutputEos) {
            // Dequeue decoded frames
            VideoFrameInfo vfi = dequeueDecodedVideoFrame();
            if(mAudioFormat != null) {
                while (mAudioPlayback.getBufferTimeUs() < 100000 && dequeueDecodedAudioFrame()) {}
            }

            // Enqueue encoded buffers into decoders
            while (!mRepresentationChanging
                    && !mVideoInputEos
                    && queueVideoSampleToCodec()) {}
            if ((mAudioFormat != null)) {
                while ((mAudioExtractor == mVideoExtractor || mAudioPlayback.getBufferTimeUs() < 100000)
                        && !mAudioInputEos
                        && queueAudioSampleToCodec(mAudioExtractor, videoOnly)) {}
            }

            if(vfi != null) {
                return vfi;
            }
        }

        Log.d(TAG, "EOS NULL");
        return null; // EOS already reached, no video frame left to return
    }

    public int getVideoWidth() {
        return mVideoFormat != null ? (int)(mVideoFormat.getInteger(MediaFormat.KEY_HEIGHT)
                * mVideoFormat.getFloat(MediaExtractor.MEDIA_FORMAT_EXTENSION_KEY_DAR)) : 0;
    }

    public int getVideoHeight() {
        return mVideoFormat != null ? mVideoFormat.getInteger(MediaFormat.KEY_HEIGHT) : 0;
    }

    /**
     * Releases all data belonging to a frame and optionally renders it to the configured surface.
     */
    public void releaseFrame(VideoFrameInfo videoFrameInfo, boolean render) {
        mVideoCodec.releaseOutputBuffer(videoFrameInfo.buffer, render); // render picture

        videoFrameInfo.clear();
        mEmptyVideoFrameInfos.add(videoFrameInfo);
    }

    /**
     * Releases all data belonging to a frame and optionally renders it to the configured surface.
     */
    @TargetApi(21)
    public void releaseFrameTimed(VideoFrameInfo videoFrameInfo, long renderOffsetUs) {
        long renderTimestampNs = System.nanoTime() + (renderOffsetUs * 1000);
        mVideoCodec.releaseOutputBuffer(videoFrameInfo.buffer, renderTimestampNs); // render picture

        videoFrameInfo.clear();
        mEmptyVideoFrameInfos.add(videoFrameInfo);
    }

    /**
     * Releases all codecs. This must be called to free decoder resources when this object is no longer in use.
     */
    public void release() {
        mVideoCodec.stop();
        mVideoCodec.release();
        if(mAudioFormat != null) {
            mAudioCodec.stop();
            mAudioCodec.release();
        }
        Log.d(TAG, "decoder released");
    }

    public VideoFrameInfo seekTo(MediaPlayer.SeekMode seekMode, long seekTargetTimeUs) throws IOException {
        Log.d(TAG, "seeking to:                 " + seekTargetTimeUs);
        Log.d(TAG, "extractor current position: " + mVideoExtractor.getSampleTime());

        mVideoExtractor.seekTo(seekTargetTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

        Log.d(TAG, "extractor new position:     " + mVideoExtractor.getSampleTime());

        // TODO add seek cancellation possibility
        // e.g. by returning an object with a cancel method and checking the flag at fitting places within this method

        mVideoInputEos = false;
        mVideoOutputEos = false;
        mAudioInputEos = false;
        mAudioOutputEos = false;
        mVideoCodec.flush();
        if (mAudioFormat != null) mAudioCodec.flush();

        if(mVideoExtractor.hasTrackFormatChanged()) {
            reinitCodecs();
            mRepresentationChanged = true;
        }

        /* Android API compatibility:
         * Use millisecond precision to stay compatible with VideoView API that works
         * in millisecond precision only. Else, exact seek matches are missed if frames
         * are positioned at fractions of a millisecond. */
        long presentationTimeMs = -1;
        long seekTargetTimeMs = seekTargetTimeUs / 1000;

        VideoFrameInfo vfi = null;

        if(seekMode == MediaPlayer.SeekMode.FAST) {
            vfi = decodeFrame(true);
            Log.d(TAG, "fast seek to " + seekTargetTimeUs + " arrived at " + vfi.presentationTimeUs);
        }
        else if (seekMode == MediaPlayer.SeekMode.FAST_EXACT) {
            fastSeek(seekTargetTimeUs);

            vfi = decodeFrame(true);
            Log.d(TAG, "fast_exact seek to " + seekTargetTimeUs + " arrived at " + vfi.presentationTimeUs);

            if(vfi.presentationTimeUs < seekTargetTimeUs) {
                Log.d(TAG, "presentation is behind...");
            }

            return vfi;
        }
        else if (seekMode == MediaPlayer.SeekMode.PRECISE || seekMode == MediaPlayer.SeekMode.EXACT) {
            /* NOTE
             * This code seeks one frame too far, except if the seek time equals the
             * frame PTS:
             * (F1.....)(F2.....)(F3.....) ... (Fn.....)
             * A frame is shown for an interval, e.g. (1/fps seconds). Now if the seek
             * target time is somewhere in frame 2's interval, we end up with frame 3
             * because we need to decode it to know if the seek target time lies in
             * frame 2's interval (because we don't know the frame rate of the video,
             * and neither if it's a fixed frame rate or a variable one - even when
             * deriving it from the PTS series we cannot be sure about it). This means
             * we always end up one frame too far, because the MediaCodec does not allow
             * to go back, except when starting at a sync frame.
             *
             * Solution for fixed frame rate could be to subtract the frame interval
             * time (1/fps secs) from the seek target time.
             *
             * Solution for variable frame rate and unknown frame rate: go back to sync
             * frame and re-seek to the now known exact PTS of the desired frame.
             * See EXACT mode handling below.
             */
            int frameSkipCount = 0;
            long lastPTS = -1;

            vfi = decodeFrame(true);
            presentationTimeMs = vfi.presentationTimeUs / 1000;

            while(presentationTimeMs < seekTargetTimeMs) {
                if(frameSkipCount == 0) {
                    Log.d(TAG, "skipping frames...");
                }
                frameSkipCount++;

                if(mVideoOutputEos) {
                    /* When the end of stream is reached while seeking, the seek target
                     * time is set to the last frame's PTS, else the seek skips the last
                     * frame which then does not get rendered, and it might end up in a
                     * loop trying to reach the unreachable target time. */
                    seekTargetTimeUs = vfi.presentationTimeUs;
                    seekTargetTimeMs = seekTargetTimeUs / 1000;
                }

                lastPTS = vfi.presentationTimeUs;
                releaseFrame(vfi, false);

                vfi = decodeFrame(true);
                presentationTimeMs = vfi.presentationTimeUs / 1000;
            }

            Log.d(TAG, "frame new position:         " + vfi.presentationTimeUs);
            Log.d(TAG, "seeking finished, skipped " + frameSkipCount + " frames");

            if(seekMode == MediaPlayer.SeekMode.EXACT && presentationTimeMs > seekTargetTimeMs) {
                if(frameSkipCount > 0) {
                    // In a single stream, the initiating seek always seeks before or directly
                    // to the requested frame, and this case never happens. With DASH, when the seek
                    // target is very near a segment border, it can happen that a wrong segment
                    // (the following one) is determined as target seek segment, which means the
                    // target of the initiating seek is too far, and we cannot go back either because
                    // it is the first frame of the segment
                    // TODO avoid this case by fixing DASH seek (fix segment calculation or reissue
                    // seek to previous segment when this case is detected)
                    Log.w(TAG, "this should never happen");
                } else {
                    /* If the current frame's PTS it after the seek target time, we're
                     * one frame too far into the stream. This is because we do not know
                     * the frame rate of the video and therefore can't decide for a frame
                     * if its interval covers the seek target time of if there's already
                     * another frame coming. We know after the next frame has been
                     * decoded though if we're too far into the stream, and if so, and if
                     * EXACT mode is desired, we need to take the previous frame's PTS
                     * and repeat the seek with that PTS to arrive at the desired frame.
                     */
                    Log.d(TAG, "exact seek: repeat seek for previous frame at " + lastPTS);
                    releaseFrame(vfi, false);
                    return seekTo(seekMode, lastPTS);
                }
            }
        }

        if(presentationTimeMs == seekTargetTimeMs) {
            Log.d(TAG, "exact seek match!");
        }

        if (mAudioExtractor != null && mAudioExtractor != mVideoExtractor) {
            mAudioExtractor.seekTo(mVideoInfo.presentationTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        }

        return vfi;
    }

    private long fastSeek(long targetTime) throws IOException {
        mVideoCodec.flush();
        if(mAudioFormat != null) mAudioCodec.flush();
        mVideoExtractor.seekTo(targetTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

        if(mVideoExtractor.getSampleTime() == targetTime) {
            Log.d(TAG, "skip fastseek, already there");
            return targetTime;
        }

        // 1. Queue first sample which should be the sync/I frame
        queueMediaSampleToCodec(true);

        // 2. Then, fast forward to target frame
        /* 2.1 Search for the best candidate frame, which is the one whose
         *     right/positive/future distance is minimized
         */
        mVideoExtractor.seekTo(targetTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

        /* Specifies how many frames we continue to check after the first candidate,
         * to account for DTS picture reordering (this value is arbitrarily chosen) */
        int maxFrameLookahead = 20;

        long candidatePTS = 0;
        long candidateDistance = Long.MAX_VALUE;
        int lookaheadCount = 0;

        while (mVideoExtractor.advance() && lookaheadCount < maxFrameLookahead) {
            long distance = targetTime - mVideoExtractor.getSampleTime();
            if (distance >= 0 && distance < candidateDistance) {
                candidateDistance = distance;
                candidatePTS = mVideoExtractor.getSampleTime();
                //Log.d(TAG, "candidate " + candidatePTS + " d=" + candidateDistance);
            }
            if (distance < 0) {
                lookaheadCount++;
            }
        }
        targetTime = candidatePTS; // set best candidate frame as exact seek target

        // 2.2 Fast forward to chosen candidate frame
        mVideoExtractor.seekTo(targetTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        while (mVideoExtractor.getSampleTime() != targetTime) {
            mVideoExtractor.advance();
        }
        Log.d(TAG, "exact fastseek match:       " + mVideoExtractor.getSampleTime());

        return targetTime;
    }
}
