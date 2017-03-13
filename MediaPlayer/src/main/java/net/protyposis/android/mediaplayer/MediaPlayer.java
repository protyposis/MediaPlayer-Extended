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

package net.protyposis.android.mediaplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by maguggen on 04.06.2014.
 */
public class MediaPlayer {

    private static final String TAG = MediaPlayer.class.getSimpleName();

    private static final long BUFFER_LOW_WATER_MARK_US = 2000000; // 2 seconds; NOTE: make sure this is below DashMediaExtractor's mMinBufferTimeUs

    /**
     * Pass as track index to tell the player that no track should be selected.
     */
    public static final int TRACK_INDEX_NONE = -1;
    /**
     * Pass as track index to tell the player to automatically select the first fitting track.
     */
    public static final int TRACK_INDEX_AUTO = -2;

    public enum SeekMode {
        /**
         * Seeks to the previous sync point.
         * This mode exists for backwards compatibility and is the same as {@link #FAST_TO_PREVIOUS_SYNC}.
         */
        @Deprecated
        FAST(MediaExtractor.SEEK_TO_PREVIOUS_SYNC),

        /**
         * Seeks to the previous sync point.
         * This seek mode equals Android MediaExtractor's {@link android.media.MediaExtractor#SEEK_TO_PREVIOUS_SYNC}.
         */
        FAST_TO_PREVIOUS_SYNC(MediaExtractor.SEEK_TO_PREVIOUS_SYNC),

        /**
         * Seeks to the next sync point.
         * This seek mode equals Android MediaExtractor's {@link android.media.MediaExtractor#SEEK_TO_NEXT_SYNC}.
         */
        FAST_TO_NEXT_SYNC(MediaExtractor.SEEK_TO_NEXT_SYNC),

        /**
         * Seeks to to the closest sync point.
         * This seek mode equals Android MediaExtractor's {@link android.media.MediaExtractor#SEEK_TO_CLOSEST_SYNC}.
         */
        FAST_TO_CLOSEST_SYNC(MediaExtractor.SEEK_TO_CLOSEST_SYNC),

        /**
         * Seeks to the exact frame if the seek time equals the frame time, else
         * to the following frame; this means that it will often seek one frame too far.
         */
        PRECISE(MediaExtractor.SEEK_TO_PREVIOUS_SYNC),

        /**
         * Default mode.
         * Always seeks to the exact frame. Can cost maximally twice the time than the PRECISE mode.
         */
        EXACT(MediaExtractor.SEEK_TO_PREVIOUS_SYNC),

        /**
         * Always seeks to the exact frame by skipping the decoding of all frames between the sync
         * and target frame, because of which it can result in block artifacts.
         */
        FAST_EXACT(MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

        private int baseSeekMode = MediaExtractor.SEEK_TO_PREVIOUS_SYNC;

        SeekMode(int baseSeekMode) {
            this.baseSeekMode = baseSeekMode;
        }

        public int getBaseSeekMode() {
            return baseSeekMode;
        }
    }

    /**
     * The mode of how to delay rendering of video frames until their target PTS.
     */
    enum VideoRenderTimingMode {

        /**
         * Automatically chooses {@link VideoRenderTimingMode#SLEEP} for API < 21 and
         * {@link VideoRenderTimingMode#SURFACEVIEW_TIMESTAMP_API21} for API >= 21.
         */
        AUTO,

        /**
         * Defers rendering by putting the playback thread to sleep until the PTS is reached and renders
         * frames through {@link MediaCodec#releaseOutputBuffer(int, boolean)}.
         */
        SLEEP,

        /**
         * Defers rendering through {@link MediaCodec#releaseOutputBuffer(int, long)} which blocks
         * until the PTS is reached. Supported on API 21+.
         */
        SURFACEVIEW_TIMESTAMP_API21;

        public boolean isRenderModeApi21() {
            switch (this) {
                case AUTO:
                    return Build.VERSION.SDK_INT >= 21;
                case SLEEP:
                    return false;
                case SURFACEVIEW_TIMESTAMP_API21:
                    return true;
            }

            return false;
        }
    }

    private enum State {
        IDLE,
        INITIALIZED,
        PREPARING,
        PREPARED,
        STOPPED,
        RELEASING,
        RELEASED,
        ERROR
    }

    private SeekMode mSeekMode = SeekMode.EXACT;
    private Surface mSurface;
    private SurfaceHolder mSurfaceHolder;
    private MediaExtractor mVideoExtractor;
    private MediaExtractor mAudioExtractor;

    private int mVideoTrackIndex;
    private MediaFormat mVideoFormat;
    private long mVideoMinPTS;

    private int mAudioTrackIndex;
    private MediaFormat mAudioFormat;
    private long mAudioMinPTS;
    private int mAudioSessionId;
    private int mAudioStreamType;
    private float mVolumeLeft = 1, mVolumeRight = 1;

    private PlaybackThread mPlaybackThread;
    private long mCurrentPosition;
    private long mSeekTargetTime;
    private boolean mSeeking;
    private int mBufferPercentage;
    private TimeBase mTimeBase;

    private EventHandler mEventHandler;
    private OnPreparedListener mOnPreparedListener;
    private OnCompletionListener mOnCompletionListener;
    private OnSeekListener mOnSeekListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;
    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;

    private PowerManager.WakeLock mWakeLock = null;
    private boolean mScreenOnWhilePlaying;
    private boolean mStayAwake;
    private boolean mLooping;

    private AudioPlayback mAudioPlayback;
    private Decoders mDecoders;
    private boolean mBuffering;
    private VideoRenderTimingMode mVideoRenderTimingMode;

    private State mCurrentState;

    /**
     * A lock to sync release() with the actual releasing on the playback thread. This lock makes
     * sure that release() waits until everything has been released before returning to the caller,
     * and thus makes the async release look synchronized to an API caller.
     */
    private Object mReleaseSyncLock;

    public MediaPlayer() {
        mPlaybackThread = null;
        mEventHandler = new EventHandler();
        mTimeBase = new TimeBase();
        mVideoRenderTimingMode = VideoRenderTimingMode.AUTO;
        mCurrentState = State.IDLE;
        mAudioSessionId = 0; // AudioSystem.AUDIO_SESSION_ALLOCATE;
        mAudioStreamType = AudioManager.STREAM_MUSIC;
    }

    /**
     * Sets the media source and track indices. The track indices can either be actual track indices
     * that have been determined externally, {@link #TRACK_INDEX_AUTO} to automatically select
     * the first fitting track index, or {@link #TRACK_INDEX_NONE} to not select any track.
     *
     * @param source the media source
     * @param videoTrackIndex a video track index or one of the TRACK_INDEX_* constants
     * @param audioTrackIndex an audio track index or one of the TRACK_INDEX_* constants
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setDataSource(MediaSource source, int videoTrackIndex, int audioTrackIndex)
            throws IOException, IllegalStateException {
        if(mCurrentState != State.IDLE) {
            throw new IllegalStateException();
        }

        mVideoExtractor = source.getVideoExtractor();
        mAudioExtractor = source.getAudioExtractor();

        if(mVideoExtractor != null && mAudioExtractor == null) {
            mAudioExtractor = mVideoExtractor;
        }

        switch (videoTrackIndex) {
            case TRACK_INDEX_AUTO:
                mVideoTrackIndex = getTrackIndex(mVideoExtractor, "video/");
                break;
            case TRACK_INDEX_NONE:
                mVideoTrackIndex = MediaCodecDecoder.INDEX_NONE;
                break;
            default:
                mVideoTrackIndex = videoTrackIndex;
        }

        switch (audioTrackIndex) {
            case TRACK_INDEX_AUTO:
                mAudioTrackIndex = getTrackIndex(mAudioExtractor, "audio/");
                break;
            case TRACK_INDEX_NONE:
                mAudioTrackIndex = MediaCodecDecoder.INDEX_NONE;
                break;
            default:
                mAudioTrackIndex = audioTrackIndex;
        }

        // Select video track
        if(mVideoTrackIndex != MediaCodecDecoder.INDEX_NONE) {
            mVideoExtractor.selectTrack(mVideoTrackIndex);
            mVideoFormat = mVideoExtractor.getTrackFormat(mVideoTrackIndex);
            mVideoMinPTS = mVideoExtractor.getSampleTime();
            Log.d(TAG, "selected video track #" + mVideoTrackIndex + " " + mVideoFormat.toString());
        }

        // Select audio track
        if(mAudioTrackIndex != MediaCodecDecoder.INDEX_NONE) {
            mAudioExtractor.selectTrack(mAudioTrackIndex);
            mAudioFormat = mAudioExtractor.getTrackFormat(mAudioTrackIndex);
            mAudioMinPTS = mAudioExtractor.getSampleTime();
            Log.d(TAG, "selected audio track #" + mAudioTrackIndex + " " + mAudioFormat.toString());
        }

        if(mVideoTrackIndex == MediaCodecDecoder.INDEX_NONE) {
            mVideoExtractor = null;
        }

        if(mVideoTrackIndex == MediaCodecDecoder.INDEX_NONE && mAudioTrackIndex == MediaCodecDecoder.INDEX_NONE) {
            throw new IOException("invalid data source, no supported stream found");
        }
        if(mVideoTrackIndex != MediaCodecDecoder.INDEX_NONE && mPlaybackThread == null && mSurface == null) {
            Log.i(TAG, "no video output surface specified");
        }

        mCurrentState = State.INITIALIZED;
    }

    /**
     * Sets the media source and automatically selects fitting tracks.
     *
     * @param source the media source
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setDataSource(MediaSource source) throws IOException, IllegalStateException {
        setDataSource(source, TRACK_INDEX_AUTO, TRACK_INDEX_AUTO);
    }

    private int getTrackIndex(MediaExtractor mediaExtractor, String mimeType) {
        if(mediaExtractor == null) {
            return MediaCodecDecoder.INDEX_NONE;
        }

        for (int i = 0; i < mediaExtractor.getTrackCount(); ++i) {
            MediaFormat format = mediaExtractor.getTrackFormat(i);
            Log.d(TAG, format.toString());
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(mimeType)) {
                return i;
            }
        }

        return MediaCodecDecoder.INDEX_NONE;
    }

    /**
     * @see android.media.MediaPlayer#setDataSource(android.content.Context, android.net.Uri, java.util.Map)
     * @deprecated only for compatibility with Android API
     */
    @Deprecated
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException {
        setDataSource(new UriSource(context, uri, headers));
    }

    /**
     * @see android.media.MediaPlayer#setDataSource(android.content.Context, android.net.Uri)
     * @deprecated only for compatibility with Android API
     */
    @Deprecated
    public void setDataSource(Context context, Uri uri) throws IOException {
        setDataSource(context, uri, null);
    }

    private void prepareInternal() throws IOException, IllegalStateException {
        MediaCodecDecoder.OnDecoderEventListener decoderEventListener = new MediaCodecDecoder.OnDecoderEventListener() {
            @Override
            public void onBuffering(MediaCodecDecoder decoder) {
                // Enter buffering mode (playback pause) if cached amount is below water mark
                // Do not enter buffering mode is player is already paused (buffering mode will be
                // entered when playback is started and buffer is too empty).
                if(mPlaybackThread != null && !mPlaybackThread.isPaused()
                        && !mBuffering
                        && mDecoders.getCachedDuration() < BUFFER_LOW_WATER_MARK_US
                        && !mDecoders.hasCacheReachedEndOfStream()) {
                    mBuffering = true;
                    mEventHandler.sendMessage(mEventHandler.obtainMessage(MEDIA_INFO,
                            MEDIA_INFO_BUFFERING_START, 0));
                }
            }
        };

        if(mCurrentState == State.RELEASING) {
            // release() has already been called, drop out of prepareAsync() (can only happen with async prepare)
            return;
        }

        mDecoders = new Decoders();

        if(mVideoTrackIndex != MediaCodecDecoder.INDEX_NONE) {
            try {
                MediaCodecDecoder vd = new MediaCodecVideoDecoder(mVideoExtractor, false, mVideoTrackIndex,
                        decoderEventListener, mSurface, mVideoRenderTimingMode.isRenderModeApi21());
                mDecoders.addDecoder(vd);
            } catch (Exception e) {
                Log.e(TAG, "cannot create video decoder: " + e.getMessage());
            }
        }

        if(mAudioTrackIndex != MediaCodecDecoder.INDEX_NONE) {
            mAudioPlayback = new AudioPlayback();
            // Initialize settings in case they have already been set before the preparation
            mAudioPlayback.setAudioSessionId(mAudioSessionId);
            setVolume(mVolumeLeft, mVolumeRight); // sets the volume on mAudioPlayback

            try {
                boolean passive = (mAudioExtractor == mVideoExtractor || mAudioExtractor == null);
                MediaCodecDecoder ad = new MediaCodecAudioDecoder(mAudioExtractor != null ? mAudioExtractor : mVideoExtractor,
                        passive, mAudioTrackIndex, decoderEventListener, mAudioPlayback);
                mDecoders.addDecoder(ad);
            } catch (Exception e) {
                Log.e(TAG, "cannot create audio decoder: " + e.getMessage());
                mAudioPlayback = null;
            }
        }

        // If no decoder could be initialized, there is nothing to play back, so we throw an exception
        if(mDecoders.getDecoders().isEmpty()) {
            throw new IOException("cannot decode any stream");
        }

        if (mAudioPlayback != null) {
            mAudioSessionId = mAudioPlayback.getAudioSessionId();
            mAudioStreamType = mAudioPlayback.getAudioStreamType();
        }

        // After the decoder is initialized, we know the video size
        if(mDecoders.getVideoDecoder() != null) {
            int width = mDecoders.getVideoDecoder().getVideoWidth();
            int height = mDecoders.getVideoDecoder().getVideoHeight();
            int rotation = mDecoders.getVideoDecoder().getVideoRotation();

            // Swap width/height to report correct dimensions of rotated portrait video (rotated by 90 or 270 degrees)
            if(rotation > 0 && rotation != 180) {
                int temp = width;
                width = height;
                height = temp;
            }

            mEventHandler.sendMessage(mEventHandler.obtainMessage(MEDIA_SET_VIDEO_SIZE, width, height));
        }

        if(mCurrentState == State.RELEASING) {
            // release() has already been called, drop out of prepareAsync()
            return;
        }

        // Decode the first frame to initialize the decoder, and seek back to the start
        // This is necessary on some platforms, else a seek directly after initialization will fail,
        // or the decoder goes into a state where it does not accept any input and does not deliver
        // any output, locking up playback (observed on N4 API22).
        // N4 API22 Test: disable this code open video, seek to end, press play to start from beginning
        //                -> results in infinite decoding loop without output
        if(true) {
            if(mDecoders.getVideoDecoder() != null) {
                MediaCodecDecoder.FrameInfo vfi = mDecoders.decodeFrame(true);
                mDecoders.getVideoDecoder().releaseFrame(vfi);
            } else {
                mDecoders.decodeFrame(false);
            }
            if (mAudioPlayback != null) mAudioPlayback.pause(true);
            mDecoders.seekTo(SeekMode.FAST_TO_PREVIOUS_SYNC, 0);
        }
    }

    /**
     * @see android.media.MediaPlayer#prepare()
     */
    public void prepare() throws IOException, IllegalStateException {
        if(mCurrentState != State.INITIALIZED && mCurrentState != State.STOPPED) {
            throw new IllegalStateException();
        }

        mCurrentState = State.PREPARING;

        // Prepare synchronously on caller thread
        prepareInternal();

        // Create the playback loop handler thread
        mPlaybackThread = new PlaybackThread();
        mPlaybackThread.start();

        mCurrentState = State.PREPARED;
    }

    /**
     * @see android.media.MediaPlayer#prepareAsync()
     */
    public void prepareAsync() throws IllegalStateException {
        if(mCurrentState != State.INITIALIZED && mCurrentState != State.STOPPED) {
            throw new IllegalStateException();
        }

        mCurrentState = State.PREPARING;

        // Create the playback loop handler thread
        mPlaybackThread = new PlaybackThread();
        mPlaybackThread.start();

        // Execute prepare asynchronously on playback thread
        mPlaybackThread.prepare();
    }

    /**
     * @see android.media.MediaPlayer#setDisplay(android.view.SurfaceHolder)
     */
    public void setDisplay(SurfaceHolder sh) {
        mSurfaceHolder = sh;
        if (sh != null) {
            mSurface = sh.getSurface();
        } else {
            mSurface = null;
        }

        if(mDecoders != null && mDecoders.getVideoDecoder() != null) {
            //mDecoders.getVideoDecoder().updateSurface(mSurface);
        }

        if(mPlaybackThread == null) {
            // Player not prepared yet, so we can set the timing mode
            setVideoRenderTimingMode(VideoRenderTimingMode.AUTO);
            updateSurfaceScreenOn();
        } else {
            // Player is already prepared, just change the surface
            mPlaybackThread.setSurface(mSurface);
        }
    }

    /**
     * @see android.media.MediaPlayer#setSurface(android.view.Surface)
     */
    public void setSurface(Surface surface) {
        mSurface = surface;
        if (mScreenOnWhilePlaying && surface != null) {
            Log.w(TAG, "setScreenOnWhilePlaying(true) is ineffective for Surface");
        }
        mSurfaceHolder = null;

        if(mPlaybackThread == null) {
            // Player not prepared yet, so we can set the timing mode
            setVideoRenderTimingMode(VideoRenderTimingMode.SLEEP); // the surface could be a GL texture, so we switch to sleep timing mode
            updateSurfaceScreenOn();
        } else {
            // Player is already prepared, just change the surface
            mPlaybackThread.setSurface(mSurface);
        }
    }

    public void start() {
        if(mCurrentState != State.PREPARED) {
            mCurrentState = State.ERROR;
            throw new IllegalStateException();
        }

        mPlaybackThread.play();
        stayAwake(true);
    }

    public void pause() {
        if(mCurrentState != State.PREPARED) {
            mCurrentState = State.ERROR;
            throw new IllegalStateException();
        }

        mPlaybackThread.pause();
        stayAwake(false);
    }

    public SeekMode getSeekMode() {
        return mSeekMode;
    }

    public void setSeekMode(SeekMode seekMode) {
        this.mSeekMode = seekMode;
    }

    public void seekTo(long usec) {
        if(mCurrentState.ordinal() < State.PREPARED.ordinal() && mCurrentState.ordinal() >= State.RELEASING.ordinal()) {
            mCurrentState = State.ERROR;
            throw new IllegalStateException();
        }

        /* A seek needs to be performed in the decoding thread to execute commands in the correct
         * order. Otherwise it can happen that, after a seek in the media decoder, seeking procedure
         * starts, then a frame is decoded, and then the codec is flushed; the PTS of the decoded frame
         * then interferes the seeking procedure, the seek stops prematurely and a wrong waiting time
         * gets calculated. */

        Log.d(TAG, "seekTo " + usec + " with video sample offset " + mVideoMinPTS);

        if (mOnSeekListener != null) {
            mOnSeekListener.onSeek(MediaPlayer.this);
        }

        mSeeking = true;
        // The passed in target time is always aligned to a zero start time, while the actual video
        // can have an offset and must not necessarily start at zero. The offset can e.g. come from
        // the CTTS box SampleOffset field, and is only reported on Android 5+. In Android 4, the
        // offset is handled by the framework, not reported, and videos always start at zero.
        // By adding the offset to the seek target time, we always seek to a zero-reference time in
        // the stream.
        mSeekTargetTime = mVideoMinPTS + usec;
        mPlaybackThread.seekTo(mSeekTargetTime);
    }

    public void seekTo(int msec) {
        seekTo(msec * 1000L);
    }

    /**
     * Sets the playback speed. Can be used for fast forward and slow motion.
     * The speed must not be negative.
     *
     * speed 0.5 = half speed / slow motion
     * speed 2.0 = double speed / fast forward
     * speed 0.0 equals to pause
     *
     * @param speed the playback speed to set
     * @throws IllegalArgumentException if the speed is negative
     */
    public void setPlaybackSpeed(float speed) {
        if(speed < 0) {
            throw new IllegalArgumentException("speed cannot be negative");
        }

        mTimeBase.setSpeed(speed);
        mTimeBase.startAt(mCurrentPosition);
    }

    /**
     * Gets the current playback speed. See {@link #setPlaybackSpeed(float)} for details.
     * @return the current playback speed
     */
    public float getPlaybackSpeed() {
        return (float)mTimeBase.getSpeed();
    }

    public boolean isPlaying() {
        if(mCurrentState.ordinal() >= State.RELEASING.ordinal()) {
            mCurrentState = State.ERROR;
            throw new IllegalStateException();
        }

        return mPlaybackThread != null && !mPlaybackThread.isPaused();
    }

    /**
     * @see android.media.MediaPlayer#setLooping(boolean)
     */
    public void setLooping(boolean looping) {
        mLooping = looping;
    }

    /**
     * @see android.media.MediaPlayer#isLooping()
     */
    public boolean isLooping() {
        return mLooping;
    }

    public void stop() {
        release();
        mCurrentState = State.STOPPED;
    }

    public void release() {
        if(mCurrentState == State.RELEASING || mCurrentState == State.RELEASED) {
            return;
        }

        mCurrentState = State.RELEASING;

        if(mPlaybackThread != null) {
            // Create a new lock object for this release cycle
            mReleaseSyncLock = new Object();

            synchronized (mReleaseSyncLock) {
                try {
                    // Schedule release on the playback thread
                    mPlaybackThread.release();
                    mPlaybackThread = null;

                    // Wait for the release on the playback thread to finish
                    mReleaseSyncLock.wait();
                } catch (InterruptedException e) {
                    // nothing to do here
                }
            }

            mReleaseSyncLock = null;
        }

        stayAwake(false);

        mCurrentState = State.RELEASED;
    }

    public void reset() {
        stop();
        mCurrentState = State.IDLE;
    }

    /**
     * @see android.media.MediaPlayer#setWakeMode(android.content.Context, int)
     */
    public void setWakeMode(Context context, int mode) {
        boolean washeld = false;
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                washeld = true;
                mWakeLock.release();
            }
            mWakeLock = null;
        }

        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(mode|PowerManager.ON_AFTER_RELEASE, MediaPlayer.class.getName());
        mWakeLock.setReferenceCounted(false);
        if (washeld) {
            mWakeLock.acquire();
        }
    }

    /**
     * @see android.media.MediaPlayer#setScreenOnWhilePlaying(boolean)
     */
    public void setScreenOnWhilePlaying(boolean screenOn) {
        if (mScreenOnWhilePlaying != screenOn) {
            if (screenOn && mSurfaceHolder == null) {
                Log.w(TAG, "setScreenOnWhilePlaying(true) is ineffective without a SurfaceHolder");
            }
            mScreenOnWhilePlaying = screenOn;
            updateSurfaceScreenOn();
        }
    }

    private void stayAwake(boolean awake) {
        if (mWakeLock != null) {
            if (awake && !mWakeLock.isHeld()) {
                mWakeLock.acquire();
            } else if (!awake && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
        mStayAwake = awake;
        updateSurfaceScreenOn();
    }

    private void updateSurfaceScreenOn() {
        if (mSurfaceHolder != null) {
            mSurfaceHolder.setKeepScreenOn(mScreenOnWhilePlaying && mStayAwake);
        }
    }

    public int getDuration() {
        if(mCurrentState.ordinal() <= State.PREPARING.ordinal() && mCurrentState.ordinal() >= State.RELEASING.ordinal()) {
            mCurrentState = State.ERROR;
            throw new IllegalStateException();
        }

        return mVideoFormat != null ? (int)(mVideoFormat.getLong(MediaFormat.KEY_DURATION)/1000) :
                mAudioFormat != null && mAudioFormat.containsKey(MediaFormat.KEY_DURATION) ? (int)(mAudioFormat.getLong(MediaFormat.KEY_DURATION)/1000) : 0;
    }

    public int getCurrentPosition() {
        if(mCurrentState.ordinal() >= State.RELEASING.ordinal()) {
            mCurrentState = State.ERROR;
            throw new IllegalStateException();
        }
        /* During a seek, return the temporary seek target time; otherwise a seek bar doesn't
         * update to the selected seek position until the seek is finished (which can take a
         * while in exact mode). */
        return (int)((mSeeking ? mSeekTargetTime : mCurrentPosition)/1000);
    }

    public int getBufferPercentage() {
        return mBufferPercentage;
    }

    public int getVideoWidth() {
        if(mCurrentState.ordinal() >= State.RELEASING.ordinal()) {
            mCurrentState = State.ERROR;
            throw new IllegalStateException();
        }

        return mVideoFormat != null ? (int)(mVideoFormat.getInteger(MediaFormat.KEY_HEIGHT)
                * mVideoFormat.getFloat(MediaExtractor.MEDIA_FORMAT_EXTENSION_KEY_DAR)) : 0;
    }

    public int getVideoHeight() {
        if(mCurrentState.ordinal() >= State.RELEASING.ordinal()) {
            mCurrentState = State.ERROR;
            throw new IllegalStateException();
        }

        return mVideoFormat != null ? mVideoFormat.getInteger(MediaFormat.KEY_HEIGHT) : 0;
    }

    /**
     * @see android.media.MediaPlayer#setVolume(float, float)
     */
    public void setVolume(float leftVolume, float rightVolume) {
        mVolumeLeft = leftVolume;
        mVolumeRight = rightVolume;

        if(mAudioPlayback != null) {
            mAudioPlayback.setStereoVolume(leftVolume, rightVolume);
        }
    }

    /**
     * This API method in the Android MediaPlayer is hidden, but may be unhidden in the future. Here
     * it can already be used.
     * see android.media.MediaPlayer#setVolume(float)
     */
    public void setVolume(float volume) {
        setVolume(volume, volume);
    }

    /**
     * @see android.media.MediaPlayer#setAudioSessionId(int)
     */
    public void setAudioSessionId(int sessionId) {
        if(mCurrentState != State.IDLE) {
            throw new IllegalStateException();
        }
        mAudioSessionId = sessionId;
    }

    /**
     * @see android.media.MediaPlayer#getAudioSessionId()
     */
    public int getAudioSessionId() {
        return mAudioSessionId;
    }

    public void setAudioStreamType(int streamType) {
        // Can be set any time, no IllegalStateException is thrown, but value will be ignored if audio is already initialized
        mAudioStreamType = streamType;
    }

    /**
     * Gets the stream type of the audio playback session.
     * @return the stream type
     */
    public int getAudioStreamType() {
        return mAudioStreamType;
    }

    /**
     * Sets the timing mode for video frame rendering.
     * This only works before the calling {@link #prepare()} or {@link #prepareAsync()}.
     *
     * This method is only needed for the special case of rendering the video to a GL surface texture,
     * where {@link MediaCodec#releaseOutputBuffer(int, long)} does not defer the frame rendering
     * and thus does not block until the PTS is reached. This only seems to work correctly on a
     * {@link android.view.SurfaceView}. It is therefore required to manually set the
     * {@link VideoRenderTimingMode#SLEEP} mode on API 21+ platforms to get timed frame rendering.
     *
     * TODO find out how to get deferred/blocking rendering to work with a surface texture
     *
     * @see VideoRenderTimingMode
     * @param mode the desired timing mode
     * @throws IllegalStateException
     */
    void setVideoRenderTimingMode(VideoRenderTimingMode mode) {
        if(mPlaybackThread != null) {
            throw new IllegalStateException("called after prepare/prepareAsync");
        }
        if(mode == VideoRenderTimingMode.SURFACEVIEW_TIMESTAMP_API21 && Build.VERSION.SDK_INT < 21) {
            throw new IllegalArgumentException("this mode needs min API 21");
        }
        Log.d(TAG, "setVideoRenderTimingMode " + mode);
        mVideoRenderTimingMode = mode;
    }

    private class PlaybackThread extends HandlerThread implements Handler.Callback {

        private static final int PLAYBACK_PREPARE = 1;
        private static final int PLAYBACK_PLAY = 2;
        private static final int PLAYBACK_PAUSE = 3;
        private static final int PLAYBACK_LOOP = 4;
        private static final int PLAYBACK_SEEK = 5;
        private static final int PLAYBACK_RELEASE = 6;
        private static final int PLAYBACK_PAUSE_AUDIO = 7;

        static final int DECODER_SET_SURFACE = 100;

        private Handler mHandler;
        private boolean mPaused;
        private boolean mReleasing;
        private MediaCodecDecoder.FrameInfo mVideoFrameInfo;
        private boolean mRenderModeApi21; // Usage of timed outputBufferRelease on API 21+
        private boolean mRenderingStarted; // Flag to know if decoding the first frame
        private double mPlaybackSpeed;
        private boolean mAVLocked;

        public PlaybackThread() {
            // Give this thread a high priority for more precise event timing
            super(TAG + "#" + PlaybackThread.class.getSimpleName(), Process.THREAD_PRIORITY_AUDIO);

            // Init fields
            mPaused = true;
            mReleasing = false;
            mRenderModeApi21 = mVideoRenderTimingMode.isRenderModeApi21();
            mRenderingStarted = true;
            mAVLocked = false;
        }

        @Override
        public synchronized void start() {
            super.start();

            // Create the handler that will process the messages on the handler thread
            mHandler = new Handler(this.getLooper(), this);

            Log.d(TAG, "PlaybackThread started");
        }

        public void prepare() {
            mHandler.sendEmptyMessage(PLAYBACK_PREPARE);
        }

        public void play() {
            mPaused = false;
            mHandler.sendEmptyMessage(PLAYBACK_PLAY);
        }

        public void pause() {
            mPaused = true;
            mHandler.sendEmptyMessage(PLAYBACK_PAUSE);
        }

        public boolean isPaused() {
            return mPaused;
        }

        public void seekTo(long usec) {
            // When multiple seek requests come in, e.g. when a user slides the finger on a
            // seek bar in the UI, we don't want to process all of them and can therefore remove
            // all requests from the queue and only keep the most recent one.
            mHandler.removeMessages(PLAYBACK_SEEK); // remove any previous requests
            mHandler.obtainMessage(PLAYBACK_SEEK, usec).sendToTarget();
        }

        public void setSurface(Surface surface) {
            mHandler.sendMessage(mHandler.obtainMessage(PlaybackThread.DECODER_SET_SURFACE, surface));
        }

        private void release() {
            if(!isAlive()) {
                return;
            }

            mPaused = true; // Set this flag so the loop does not schedule next loop iteration
            mReleasing = true;

            // Call actual release method
            // Actually it does not matter what we schedule here, we just need to schedule
            // something so {@link #handleMessage} gets called on the handler thread, read the
            // mReleasing flag, and call {@link #releaseInternal}.
            mHandler.sendEmptyMessage(PLAYBACK_RELEASE);
        }

        @Override
        public boolean handleMessage(Message msg) {
            try {
                if(mReleasing) {
                    // When the releasing flag is set, just release without processing any more messages
                    releaseInternal();
                    return true;
                }

                switch (msg.what) {
                    case PLAYBACK_PREPARE:
                        prepareInternal();
                        return true;
                    case PLAYBACK_PLAY:
                        playInternal();
                        return true;
                    case PLAYBACK_PAUSE:
                        pauseInternal();
                        return true;
                    case PLAYBACK_PAUSE_AUDIO:
                        pauseInternalAudio();
                        return true;
                    case PLAYBACK_LOOP:
                        loopInternal();
                        return true;
                    case PLAYBACK_SEEK:
                        seekInternal((Long) msg.obj);
                        return true;
                    case PLAYBACK_RELEASE:
                        releaseInternal();
                        return true;
                    case DECODER_SET_SURFACE:
                        setVideoSurface((Surface) msg.obj);
                        return true;
                    default:
                        Log.d(TAG, "unknown/invalid message");
                        return false;
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "decoder interrupted", e);
                mEventHandler.sendMessage(mEventHandler.obtainMessage(MEDIA_ERROR,
                        MEDIA_ERROR_UNKNOWN, 0));
            } catch (IllegalStateException e) {
                Log.e(TAG, "decoder error, too many instances?", e);
                mEventHandler.sendMessage(mEventHandler.obtainMessage(MEDIA_ERROR,
                        MEDIA_ERROR_UNKNOWN, 0));
            } catch (IOException e) {
                Log.e(TAG, "decoder error, codec can not be created", e);
                mEventHandler.sendMessage(mEventHandler.obtainMessage(MEDIA_ERROR,
                        MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO));
            }

            // Release after an exception
            releaseInternal();
            return true;
        }

        private void prepareInternal() {
            try {
                MediaPlayer.this.prepareInternal();
                mCurrentState = MediaPlayer.State.PREPARED;

                // This event is only triggered after a successful async prepare (not after the sync prepare!)
                mEventHandler.sendEmptyMessage(MEDIA_PREPARED);
            } catch (IOException e) {
                Log.e(TAG, "prepareAsync() failed: cannot decode stream(s)", e);
                mEventHandler.sendMessage(mEventHandler.obtainMessage(MEDIA_ERROR,
                        MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO));
                releaseInternal();
            } catch (IllegalStateException e) {
                Log.e(TAG, "prepareAsync() failed: something is in a wrong state", e);
                mEventHandler.sendMessage(mEventHandler.obtainMessage(MEDIA_ERROR,
                        MEDIA_ERROR_UNKNOWN, 0));
                releaseInternal();
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "prepareAsync() failed: surface might be gone", e);
                mEventHandler.sendMessage(mEventHandler.obtainMessage(MEDIA_ERROR,
                        MEDIA_ERROR_UNKNOWN, 0));
                releaseInternal();
            }
        }

        private void playInternal() throws IOException, InterruptedException {
            if(mDecoders.isEOS()) {
                mCurrentPosition = 0;
                mDecoders.seekTo(SeekMode.FAST_TO_PREVIOUS_SYNC, 0);
            }

            // reset time (otherwise playback tries to "catch up" time after a pause)
            mTimeBase.startAt(mDecoders.getCurrentDecodingPTS());

            if(mAudioPlayback != null) {
                mHandler.removeMessages(PLAYBACK_PAUSE_AUDIO);
                mAudioPlayback.play();
            }

            mPlaybackSpeed = mTimeBase.getSpeed();
            // Sync audio playback speed to playback speed (to account for speed changes during pause)
            if (mAudioPlayback != null) {
                mAudioPlayback.setPlaybackSpeed((float) mPlaybackSpeed);
            }

            mHandler.removeMessages(PLAYBACK_LOOP);
            loopInternal();
        }

        private void pauseInternal(boolean drainAudioPlayback) {
            // When playback is paused in timed API21 render mode, the remaining cached frames will
            // still be rendered, resulting in a short but noticeable pausing lag. This can be avoided
            // by switching to the old render timing mode.
            mHandler.removeMessages(PLAYBACK_LOOP); // removes remaining loop requests (required when EOS is reached)
            if (mAudioPlayback != null) {
                if(drainAudioPlayback) {
                    // Defer pausing the audio playback for the length of the playback buffer, to
                    // make sure that all audio samples have been played out.
                    mHandler.sendEmptyMessageDelayed(PLAYBACK_PAUSE_AUDIO,
                            (mAudioPlayback.getQueueBufferTimeUs() + mAudioPlayback.getPlaybackBufferTimeUs()) / 1000 + 1);
                } else {
                    mAudioPlayback.pause(false);
                }
            }
        }

        private void pauseInternal() {
            pauseInternal(false);
        }

        private void pauseInternalAudio() {
            if (mAudioPlayback != null) {
                mAudioPlayback.pause();
            }
        }

        private void loopInternal() throws IOException, InterruptedException {
            // If this is an online stream, notify the client of the buffer fill level.
            long cachedDuration = mDecoders.getCachedDuration();
            if(cachedDuration != -1) {
                // The cached duration from the MediaExtractor returns the cached time from
                // the current position onwards, but the Android MediaPlayer returns the
                // total time consisting of the current playback point and the length of
                // the prefetched data.
                // This comes before the buffering pause to update the clients buffering info
                // also during a buffering playback pause.
                mEventHandler.sendMessage(mEventHandler.obtainMessage(MEDIA_BUFFERING_UPDATE,
                        (int) (100d / (getDuration() * 1000) * (mCurrentPosition + cachedDuration)), 0));
            }

            // If we are in buffering mode, check if the buffer has been filled until the low water
            // mark or the end of the stream has been reached, and pause playback if it isn't filled
            // high enough yet.
            if(mBuffering && cachedDuration > -1 && cachedDuration < BUFFER_LOW_WATER_MARK_US && !mDecoders.hasCacheReachedEndOfStream()) {
                //Log.d(TAG, "buffering... " + mDecoders.getCachedDuration() + " / " + BUFFER_LOW_WATER_MARK_US);
                // To pause playback for buffering, we simply skip this loop and call it again later
                mHandler.sendEmptyMessageDelayed(PLAYBACK_LOOP, 100);
                return;
            }

            if(mDecoders.getVideoDecoder() != null && mVideoFrameInfo == null) {
                // This method needs a video frame to operate on. If there is no frame, we need
                // to decode one first.
                mVideoFrameInfo = mDecoders.decodeFrame(false);
                if(mVideoFrameInfo == null && !mDecoders.isEOS()) {
                    // If the decoder didn't return a frame, we need to give it some processing time
                    // and come back later...
                    mHandler.sendEmptyMessageDelayed(PLAYBACK_LOOP, 10);
                    return;
                }
            }

            long startTime = SystemClock.elapsedRealtime();

            // When we are in buffering mode, and a frame has been decoded, the buffer is
            // obviously refilled so we can send the buffering end message and exit buffering mode.
            if(mBuffering) {
                mBuffering = false;
                mEventHandler.sendMessage(mEventHandler.obtainMessage(MEDIA_INFO,
                        MEDIA_INFO_BUFFERING_END, 0));

                // Reset timebase so player does not try to catch up time lost while caching
                mTimeBase.startAt(mDecoders.getCurrentDecodingPTS());
            }

            // When the waiting time to the next frame is too long, we defer rendering through
            // the handler here instead of relying on releaseOutputBuffer(buffer, renderTimestampNs),
            // which does not work well with long waiting times and many frames in the queue.
            // On API < 21 the frame rendering is timed with a sleep() and this is not really necessary,
            // but still shifts some waiting time from the sleep() to here.
            if(mVideoFrameInfo != null && mTimeBase.getOffsetFrom(mVideoFrameInfo.presentationTimeUs) > 60000) {
                mHandler.sendEmptyMessageDelayed(PLAYBACK_LOOP, 50);
                return;
            }

            // Update the current position of the player
            mCurrentPosition = mDecoders.getCurrentDecodingPTS();

            if(mDecoders.getVideoDecoder() != null && mVideoFrameInfo != null) {
                renderVideoFrame(mVideoFrameInfo);
                mVideoFrameInfo = null;

                // When the first frame is rendered, video rendering has started and the event triggered
                if (mRenderingStarted) {
                    mRenderingStarted = false;
                    mEventHandler.sendMessage(mEventHandler.obtainMessage(MEDIA_INFO,
                            MEDIA_INFO_VIDEO_RENDERING_START, 0));
                }
            }

            if (mAudioPlayback != null) {
                // Sync audio playback speed to playback speed (to account for speed changes during playback)
                // Change the speed on the audio playback object only if it has really changed, to avoid runtime overhead
                if(mPlaybackSpeed != mTimeBase.getSpeed()) {
                    mPlaybackSpeed = mTimeBase.getSpeed();
                    mAudioPlayback.setPlaybackSpeed((float) mPlaybackSpeed);
                }

                // Sync timebase to audio timebase when there is audio data available
                long currentAudioPTS = mAudioPlayback.getCurrentPresentationTimeUs();
                if(currentAudioPTS > AudioPlayback.PTS_NOT_SET) {
                    mTimeBase.startAt(currentAudioPTS);
                }
            }

            // Handle EOS
            if (mDecoders.isEOS()) {
                mEventHandler.sendEmptyMessage(MEDIA_PLAYBACK_COMPLETE);

                // If looping is on, seek back to the start...
                if(mLooping) {
                    if(mAudioPlayback != null) {
                        // Flush audio buffer to reset audio PTS
                        mAudioPlayback.flush();
                    }
                    mDecoders.seekTo(SeekMode.FAST_TO_PREVIOUS_SYNC, 0);
                    mDecoders.renderFrames();
                }
                // ... else just pause playback and wait for next command
                else {
                    mPaused = true;
                    pauseInternal(true); // pause but play remaining buffered audio
                }
            } else {
                // Get next frame
                mVideoFrameInfo = mDecoders.decodeFrame(false);
            }

            if(!mPaused) {
                // Static delay time until the next call of the playback loop
                long delay = 10;
                // Scale delay by playback speed to avoid limiting framerate
                delay = (long)(delay / mTimeBase.getSpeed());
                // Calculate the duration taken for the current call
                long duration = (SystemClock.elapsedRealtime() - startTime);
                // Adjust the delay by the time taken
                delay = delay - duration;
                if(delay > 0) {
                    // Sleep for some time and then continue processing the loop
                    // This replaces the very unreliable and jittery Thread.sleep in the old decoder thread
                    mHandler.sendEmptyMessageDelayed(PLAYBACK_LOOP, delay);
                } else {
                    // The current call took too much time; there is no time left for delaying, call instantly
                    mHandler.sendEmptyMessage(PLAYBACK_LOOP);
                }
            }
        }

        private void seekInternal(long usec) throws IOException, InterruptedException {
            if(mVideoFrameInfo != null) {
                // A decoded video frame is waiting to be rendered, dismiss it
                mDecoders.getVideoDecoder().dismissFrame(mVideoFrameInfo);
                mVideoFrameInfo = null;
            }

            // Clear the audio cache
            if(mAudioPlayback != null) mAudioPlayback.pause(true);

            // Seek to the target time
            mDecoders.seekTo(mSeekMode, usec);

            // Reset time to keep frame rate constant
            // (otherwise it's too fast on back seeks and waits for the PTS time on fw seeks)
            mTimeBase.startAt(mDecoders.getCurrentDecodingPTS());

            // Check if another seek has been issued in the meantime
            boolean newSeekWaiting = mHandler.hasMessages(PLAYBACK_SEEK);

            // Render seek target frame (if no new seek is waiting to be processed)
            if(newSeekWaiting) {
                mDecoders.dismissFrames();
            } else {
                mDecoders.renderFrames();
            }

            // When there are no more seek requests in the queue, notify of finished seek operation
            if(!newSeekWaiting) {
                // Set the final seek position as the current position
                // (the final seek position may be off the initial target seek position)
                mCurrentPosition = mDecoders.getCurrentDecodingPTS();
                mSeeking = false;
                mAVLocked = false;

                mEventHandler.sendEmptyMessage(MEDIA_SEEK_COMPLETE);

                if(!mPaused) {
                    playInternal();
                }
            }
        }

        private void releaseInternal() {
            // post interrupt to avoid all further execution of messages/events in the queue
            interrupt();

            // quit message processing and exit thread
            quit();

            if(mDecoders != null) {
                if(mVideoFrameInfo != null) {
                    mDecoders.getVideoDecoder().releaseFrame(mVideoFrameInfo);
                    mVideoFrameInfo = null;
                }
            }

            if(mDecoders != null) {
                mDecoders.release();
            }
            if(mAudioPlayback != null) mAudioPlayback.stopAndRelease();
            if(mAudioExtractor != null & mAudioExtractor != mVideoExtractor) {
                mAudioExtractor.release();
            }
            if(mVideoExtractor != null) mVideoExtractor.release();

            Log.d(TAG, "PlaybackThread destroyed");

            // Notify #release() that it can now continue because #releaseInternal is finished
            if(mReleaseSyncLock != null) {
                synchronized(mReleaseSyncLock) {
                    mReleaseSyncLock.notify();
                    mReleaseSyncLock = null;
                }
            }
        }

        private void renderVideoFrame(MediaCodecDecoder.FrameInfo videoFrameInfo) throws InterruptedException {
            if(videoFrameInfo.endOfStream) {
                // The EOS frame does not contain a video frame, so we dismiss it
                mDecoders.getVideoDecoder().dismissFrame(videoFrameInfo);
                return;
            }

            // Calculate waiting time until the next frame's PTS
            // The waiting time might be much higher that a frame's duration because timed API21
            // rendering caches multiple released output frames before actually rendering them.
            long waitingTime = mTimeBase.getOffsetFrom(videoFrameInfo.presentationTimeUs);
//            Log.d(TAG, "VPTS " + mCurrentPosition
//                    + " APTS " + mAudioPlayback.getCurrentPresentationTimeUs()
//                    + " waitingTime " + waitingTime);

            if (waitingTime < -1000) {
                // we need to catch up time by skipping rendering of this frame
                // this doesn't gain enough time if playback speed is too high and decoder at full load
                // TODO improve fast forward mode
                Log.d(TAG, "LAGGING " + waitingTime);
                mEventHandler.sendMessage(mEventHandler.obtainMessage(MEDIA_INFO,
                        MEDIA_INFO_VIDEO_TRACK_LAGGING, 0));
            }

            // Defer the video size changed message until the first frame of the new size is being rendered
            if (videoFrameInfo.representationChanged) {
                mEventHandler.sendMessage(mEventHandler.obtainMessage(MEDIA_SET_VIDEO_SIZE,
                        mDecoders.getVideoDecoder().getVideoWidth(), mDecoders.getVideoDecoder().getVideoHeight()));
            }

            // Slow down playback, if necessary, to keep frame rate
            if(!mRenderModeApi21 && waitingTime > 5000) {
                // Sleep until it's time to render the next frame
                // This is not v-synced to the display. Not required any more on API 21+.
                Thread.sleep(waitingTime / 1000);
            }
            // Release the current frame and render it to the surface
            mDecoders.getVideoDecoder().renderFrame(videoFrameInfo, waitingTime);
        }

        private void setVideoSurface(Surface surface) {
            if(mDecoders != null && mDecoders.getVideoDecoder() != null) {
                if(mVideoFrameInfo != null) {
                    // Dismiss queued video frame
                    // After updating the surface, which re-initializes the codec,
                    // the frame buffer will not be valid any more and trying to decode
                    // it would result in an error; so we throw it away.
                    mDecoders.getVideoDecoder().dismissFrame(mVideoFrameInfo);
                    mVideoFrameInfo = null;
                }

                mDecoders.getVideoDecoder().updateSurface(surface);
            }
        }
    }

    /**
     * Interface definition for a callback to be invoked when the media
     * source is ready for playback.
     */
    public interface OnPreparedListener {
        /**
         * Called when the media file is ready for playback.
         * @param mp the MediaPlayer that is ready for playback
         */
        void onPrepared(MediaPlayer mp);
    }

    /**
     * Register a callback to be invoked when the media source is ready
     * for playback.
     *
     * @param listener the callback that will be run
     */
    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    /**
     * Interface definition for a callback to be invoked when playback of
     * a media source has completed.
     */
    public interface OnCompletionListener {
        /**
         * Called when the end of a media source is reached during playback.
         * @param mp the MediaPlayer that reached the end of the file
         */
        void onCompletion(MediaPlayer mp);
    }

    /**
     * Register a callback to be invoked when the end of a media source
     * has been reached during playback.
     *
     * @param listener the callback that will be run
     */
    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    /**
     * Interface definition of a callback to be invoked when a seek
     * is issued.
     */
    public interface OnSeekListener {
        /**
         * Called to indicate that a seek operation has been started.
         * @param mp the mediaPlayer that the seek was called on
         */
        public void onSeek(MediaPlayer mp);
    }

    /**
     * Register a calback to be invoked when a seek operation has been started.
     * @param listener the callback that will be run
     */
    public void setOnSeekListener(OnSeekListener listener) {
        mOnSeekListener = listener;
    }

    /**
     * Interface definition of a callback to be invoked indicating
     * the completion of a seek operation.
     */
    public interface OnSeekCompleteListener {
        /**
         * Called to indicate the completion of a seek operation.
         * @param mp the MediaPlayer that issued the seek operation
         */
        public void onSeekComplete(MediaPlayer mp);
    }

    /**
     * Register a callback to be invoked when a seek operation has been
     * completed.
     *
     * @param listener the callback that will be run
     */
    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        mOnSeekCompleteListener = listener;
    }

    /**
     * Interface definition of a callback to be invoked when the
     * video size is first known or updated
     */
    public interface OnVideoSizeChangedListener
    {
        /**
         * Called to indicate the video size
         *
         * The video size (width and height) could be 0 if there was no video,
         * no display surface was set, or the value was not determined yet.
         *
         * @param mp        the MediaPlayer associated with this callback
         * @param width     the width of the video
         * @param height    the height of the video
         */
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height);
    }

    /**
     * Register a callback to be invoked when the video size is
     * known or updated.
     *
     * @param listener the callback that will be run
     */
    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
        mOnVideoSizeChangedListener = listener;
    }

    /**
     * Interface definition of a callback to be invoked indicating buffering
     * status of a media resource being streamed over the network.
     */
    public interface OnBufferingUpdateListener {
        /**
         * Called to update status in buffering a media stream received through
         * progressive HTTP download. The received buffering percentage
         * indicates how much of the content has been buffered or played.
         * For example a buffering update of 80 percent when half the content
         * has already been played indicates that the next 30 percent of the
         * content to play has been buffered.
         *
         * @param mp      the MediaPlayer the update pertains to
         * @param percent the percentage (0-100) of the content
         *                that has been buffered or played thus far
         */
        void onBufferingUpdate(MediaPlayer mp, int percent);
    }

    /**
     * Register a callback to be invoked when the status of a network
     * stream's buffer has changed.
     *
     * @param listener the callback that will be run.
     */
    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        mOnBufferingUpdateListener = listener;
    }

    /** Unspecified media player error.
     * @see MediaPlayer.OnErrorListener
     */
    public static final int MEDIA_ERROR_UNKNOWN = 1;

    /** Media server died. In this case, the application must release the
     * MediaPlayer object and instantiate a new one.
     * @see MediaPlayer.OnErrorListener
     */
    public static final int MEDIA_ERROR_SERVER_DIED = 100;

    /** The video is streamed and its container is not valid for progressive
     * playback i.e the video's index (e.g moov atom) is not at the start of the
     * file.
     * @see MediaPlayer.OnErrorListener
     */
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;

    /** File or network related operation errors. */
    public static final int MEDIA_ERROR_IO = -1004;
    /** Bitstream is not conforming to the related coding standard or file spec. */
    public static final int MEDIA_ERROR_MALFORMED = -1007;
    /** Bitstream is conforming to the related coding standard or file spec, but
     * the media framework does not support the feature. */
    public static final int MEDIA_ERROR_UNSUPPORTED = -1010;
    /** Some operation takes too long to complete, usually more than 3-5 seconds. */
    public static final int MEDIA_ERROR_TIMED_OUT = -110;

    /**
     * Interface definition of a callback to be invoked when there
     * has been an error during an asynchronous operation (other errors
     * will throw exceptions at method call time).
     */
    public interface OnErrorListener
    {
        /**
         * Called to indicate an error.
         *
         * @param mp      the MediaPlayer the error pertains to
         * @param what    the type of error that has occurred:
         * <ul>
         * <li>{@link #MEDIA_ERROR_UNKNOWN}
         * <li>{@link #MEDIA_ERROR_SERVER_DIED}
         * </ul>
         * @param extra an extra code, specific to the error. Typically
         * implementation dependent.
         * <ul>
         * <li>{@link #MEDIA_ERROR_IO}
         * <li>{@link #MEDIA_ERROR_MALFORMED}
         * <li>{@link #MEDIA_ERROR_UNSUPPORTED}
         * <li>{@link #MEDIA_ERROR_TIMED_OUT}
         * </ul>
         * @return True if the method handled the error, false if it didn't.
         * Returning false, or not having an OnErrorListener at all, will
         * cause the OnCompletionListener to be called.
         */
        boolean onError(MediaPlayer mp, int what, int extra);
    }

    /**
     * Register a callback to be invoked when an error has happened
     * during an asynchronous operation.
     *
     * @param listener the callback that will be run
     */
    public void setOnErrorListener(OnErrorListener listener)
    {
        mOnErrorListener = listener;
    }

    /** The player just pushed the very first video frame for rendering.
     * @see MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_VIDEO_RENDERING_START = 3;

    /** The video is too complex for the decoder: it can't decode frames fast
     *  enough. Possibly only the audio plays fine at this stage.
     * @see MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;

    /** MediaPlayer is temporarily pausing playback internally in order to
     * buffer more data.
     * @see MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_BUFFERING_START = 701;

    /** MediaPlayer is resuming playback after filling buffers.
     * @see MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_BUFFERING_END = 702;

    /**
     * Interface definition of a callback to be invoked to communicate some
     * info and/or warning about the media or its playback.
     */
    public interface OnInfoListener {
        /**
         * Called to indicate an info or a warning.
         *
         * @param mp      the MediaPlayer the info pertains to.
         * @param what    the type of info or warning.
         * <ul>
         * <li>{@link #MEDIA_INFO_VIDEO_TRACK_LAGGING}
         * <li>{@link #MEDIA_INFO_VIDEO_RENDERING_START}
         * <li>{@link #MEDIA_INFO_BUFFERING_START}
         * <li>{@link #MEDIA_INFO_BUFFERING_END}
         * </ul>
         * @param extra an extra code, specific to the info. Typically
         * implementation dependent.
         * @return True if the method handled the info, false if it didn't.
         * Returning false, or not having an OnErrorListener at all, will
         * cause the info to be discarded.
         */
        boolean onInfo(MediaPlayer mp, int what, int extra);
    }

    /**
     * Register a callback to be invoked when an info/warning is available.
     * @param listener the callback that will be run
     */
    public void setOnInfoListener(OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    private static final int MEDIA_PREPARED = 1;
    private static final int MEDIA_PLAYBACK_COMPLETE = 2;
    private static final int MEDIA_BUFFERING_UPDATE = 3;
    private static final int MEDIA_SEEK_COMPLETE = 4;
    private static final int MEDIA_SET_VIDEO_SIZE = 5;
    private static final int MEDIA_ERROR = 100;
    private static final int MEDIA_INFO = 200;

    private class EventHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MEDIA_PREPARED:
                    Log.d(TAG, "onPrepared");
                    if(mOnPreparedListener != null) {
                        mOnPreparedListener.onPrepared(MediaPlayer.this);
                    }
                    return;
                case MEDIA_SEEK_COMPLETE:
                    Log.d(TAG, "onSeekComplete");
                    if (mOnSeekCompleteListener != null) {
                        mOnSeekCompleteListener.onSeekComplete(MediaPlayer.this);
                    }
                    return;
                case MEDIA_PLAYBACK_COMPLETE:
                    Log.d(TAG, "onPlaybackComplete");
                    if(mOnCompletionListener != null) {
                        mOnCompletionListener.onCompletion(MediaPlayer.this);
                    }
                    stayAwake(false);
                    return;
                case MEDIA_SET_VIDEO_SIZE:
                    Log.d(TAG, "onVideoSizeChanged");
                    if(mOnVideoSizeChangedListener != null) {
                        mOnVideoSizeChangedListener.onVideoSizeChanged(MediaPlayer.this, msg.arg1, msg.arg2);
                    }
                    return;
                case MEDIA_ERROR:
                    Log.e(TAG, "Error (" + msg.arg1 + "," + msg.arg2 + ")");
                    boolean error_was_handled = false;
                    if (mOnErrorListener != null) {
                        error_was_handled = mOnErrorListener.onError(MediaPlayer.this, msg.arg1, msg.arg2);
                    }
                    if (mOnCompletionListener != null && !error_was_handled) {
                        mOnCompletionListener.onCompletion(MediaPlayer.this);
                    }
                    stayAwake(false);
                    return;
                case MEDIA_INFO:
                    Log.d(TAG, "onInfo");
                    if(mOnInfoListener != null) {
                        mOnInfoListener.onInfo(MediaPlayer.this, msg.arg1, msg.arg2);
                    }
                    return;
                case MEDIA_BUFFERING_UPDATE:
                    //Log.d(TAG, "onBufferingUpdate");
                    if (mOnBufferingUpdateListener != null)
                        mOnBufferingUpdateListener.onBufferingUpdate(MediaPlayer.this, msg.arg1);
                    mBufferPercentage = msg.arg1;
                    return;

                default:
                    // nothing
            }
        }
    }
}
