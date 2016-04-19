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

package at.aau.itec.android.mediaplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.MediaController;
import android.widget.Toast;

import java.io.IOException;
import java.util.Map;

/**
 * Created by maguggen on 04.06.2014.
 */
public class VideoView extends SurfaceView implements SurfaceHolder.Callback,
        MediaController.MediaPlayerControl {

    private static final String TAG = VideoView.class.getSimpleName();

    private MediaSource mSource;
    private MediaPlayer mPlayer;
    private SurfaceHolder mSurfaceHolder;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mSeekWhenPrepared;

    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private MediaPlayer.OnSeekListener mOnSeekListener;
    private MediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener;
    private MediaPlayer.OnCompletionListener mOnCompletionListener;
    private MediaPlayer.OnErrorListener mOnErrorListener;
    private MediaPlayer.OnInfoListener mOnInfoListener;
    private MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener;

    public VideoView(Context context) {
        super(context);
        initVideoView();
    }

    public VideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView();
    }

    public VideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initVideoView();
    }

    private void initVideoView() {
        getHolder().addCallback(this);
    }

    public void setVideoSource(MediaSource source) {
        mSource = source;
        mSeekWhenPrepared = 0;
        openVideo();
        requestLayout();
        invalidate();
    }

    /**
     * @see android.widget.VideoView#setVideoPath(String)
     * @param path
     * @deprecated only for compatibility with Android API
     */
    @Deprecated
    public void setVideoPath(String path) {
        setVideoSource(new UriSource(getContext(), Uri.parse(path)));
    }

    /**
     * @see android.widget.VideoView#setVideoURI(android.net.Uri)
     * @param uri
     * @deprecated only for compatibility with Android API
     */
    @Deprecated
    public void setVideoURI(Uri uri) {
        setVideoSource(new UriSource(getContext(), uri));
    }

    /**
     * @see android.widget.VideoView#setVideoURI(android.net.Uri, Map)
     * @param uri
     * @param headers
     * @deprecated only for compatibility with Android API
     */
    @Deprecated
    public void setVideoURI(Uri uri, Map<String, String> headers) {
        setVideoSource(new UriSource(getContext(), uri, headers));
    }

    public MediaPlayer getMediaPlayer() {
        // TODO do not return the real media player
        // Handling width it could result in invalid states, better return a "censored" wrapper interface
        return mPlayer;
    }

    private void openVideo() {
        if (mSource == null || mSurfaceHolder == null) {
            // not ready for playback yet, will be called again later
            return;
        }

        release();

        mPlayer = new MediaPlayer();
        mPlayer.setDisplay(mSurfaceHolder);
        mPlayer.setScreenOnWhilePlaying(true);
        mPlayer.setOnPreparedListener(mPreparedListener);
        mPlayer.setOnSeekListener(mSeekListener);
        mPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
        mPlayer.setOnCompletionListener(mCompletionListener);
        mPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
        mPlayer.setOnErrorListener(mErrorListener);
        mPlayer.setOnInfoListener(mInfoListener);
        mPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);

        // Create a handler for the error message in case an exceptions happens in the following thread
        final Handler exceptionHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                mErrorListener.onError(mPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
                return true;
            }
        });

        // Set the data source asynchronously as this might take a while, e.g. is data has to be
        // requested from the network/internet.
        // IMPORTANT:
        // We use a Thread instead of an AsyncTask for performance reasons, because threads started
        // in an AsyncTask perform much worse, no matter the priority the Thread gets (unless the
        // AsyncTask's priority is elevated before creating the Thread).
        // See comment in MediaPlayer#prepareAsync for detailed explanation.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mPlayer.setDataSource(mSource);

                    // Async prepare spawns another thread inside this thread which really isn't
                    // necessary; we call this method anyway because of the events it triggers
                    // when it fails, and to stay in sync which the Android VideoView that does
                    // the same.
                    mPlayer.prepareAsync();

                    Log.d(TAG, "video opened");
                } catch (IOException e) {
                    Log.e(TAG, "video open failed", e);

                    // Send message to the handler that an error occurred
                    // (we don't need a message id as the handler only handles this single message)
                    exceptionHandler.sendEmptyMessage(0);
                }
            }
        }).start();
    }

    /**
     * Resizes the video view according to the video size to keep aspect ratio.
     * Code copied from {@link android.widget.VideoView#onMeasure(int, int)}.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Log.i("@@@@", "onMeasure(" + MeasureSpec.toString(widthMeasureSpec) + ", "
        //        + MeasureSpec.toString(heightMeasureSpec) + ")");

        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        if (mVideoWidth > 0 && mVideoHeight > 0) {

            int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
                // the size is fixed
                width = widthSpecSize;
                height = heightSpecSize;

                // for compatibility, we adjust size based on aspect ratio
                if ( mVideoWidth * height  < width * mVideoHeight ) {
                    //Log.i("@@@", "image too wide, correcting");
                    width = height * mVideoWidth / mVideoHeight;
                } else if ( mVideoWidth * height  > width * mVideoHeight ) {
                    //Log.i("@@@", "image too tall, correcting");
                    height = width * mVideoHeight / mVideoWidth;
                }
            } else if (widthSpecMode == MeasureSpec.EXACTLY) {
                // only the width is fixed, adjust the height to match aspect ratio if possible
                width = widthSpecSize;
                height = width * mVideoHeight / mVideoWidth;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    height = heightSpecSize;
                }
            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
                // only the height is fixed, adjust the width to match aspect ratio if possible
                height = heightSpecSize;
                width = height * mVideoWidth / mVideoHeight;
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    width = widthSpecSize;
                }
            } else {
                // neither the width nor the height are fixed, try to use actual video size
                width = mVideoWidth;
                height = mVideoHeight;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // too tall, decrease both width and height
                    height = heightSpecSize;
                    width = height * mVideoWidth / mVideoHeight;
                }
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // too wide, decrease both width and height
                    width = widthSpecSize;
                    height = width * mVideoHeight / mVideoWidth;
                }
            }
        } else {
            // no size yet, just adopt the given spec sizes
        }
        setMeasuredDimension(width, height);
    }

    private void release() {
        if(mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    public void setOnPreparedListener(MediaPlayer.OnPreparedListener l) {
        this.mOnPreparedListener = l;
    }

    public void setOnSeekListener(MediaPlayer.OnSeekListener l) {
        this.mOnSeekListener = l;
    }

    public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener l) {
        this.mOnSeekCompleteListener = l;
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener l) {
        this.mOnCompletionListener = l;
    }

    public void setOnBufferingUpdateListener(MediaPlayer.OnBufferingUpdateListener l) {
        this.mOnBufferingUpdateListener = l;
    }

    public void setOnErrorListener(MediaPlayer.OnErrorListener l) {
        this.mOnErrorListener = l;
    }

    public void setOnInfoListener(MediaPlayer.OnInfoListener l) {
        this.mOnInfoListener = l;
    }

    @Override
    public void start() {
        mPlayer.start();
    }

    @Override
    public void pause() {
        mPlayer.pause();
    }

    public void stopPlayback() {
        mPlayer.stop();
    }

    public void setPlaybackSpeed(float speed) {
        mPlayer.setPlaybackSpeed(speed);
    }

    public float getPlaybackSpeed() {
        return mPlayer.getPlaybackSpeed();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        openVideo();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // nothing yet
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder = null;
        release();
    }

    @Override
    public int getDuration() {
        return mPlayer != null ? mPlayer.getDuration() : 0;
    }

    @Override
    public int getCurrentPosition() {
        return mPlayer != null ? mPlayer.getCurrentPosition() : 0;
    }

    @Override
    public void seekTo(int msec) {
        if(mPlayer != null) {
            mPlayer.seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }

    public MediaPlayer.SeekMode getSeekMode() {
        return mPlayer.getSeekMode();
    }

    public void setSeekMode(MediaPlayer.SeekMode seekMode) {
        mPlayer.setSeekMode(seekMode);
    }

    @Override
    public boolean isPlaying() {
        return mPlayer != null && mPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return mPlayer != null ? mPlayer.getBufferPercentage() : 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return mPlayer != null ? mPlayer.getAudioSessionId() : 0;
    }

    private MediaPlayer.OnPreparedListener mPreparedListener =
            new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            if(mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(mp);
            }

            int seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
            if (seekToPosition != 0) {
                seekTo(seekToPosition);
            }
        }
    };

    private MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
            new MediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
            mVideoWidth = width;
            mVideoHeight = height;
            requestLayout();
        }
    };

    private MediaPlayer.OnSeekListener mSeekListener = new MediaPlayer.OnSeekListener() {
        @Override
        public void onSeek(MediaPlayer mp) {
            if(mOnSeekListener != null) {
                mOnSeekListener.onSeek(mp);
            }
        }
    };

    private MediaPlayer.OnSeekCompleteListener mSeekCompleteListener =
            new MediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(MediaPlayer mp) {
            if(mOnSeekCompleteListener != null) {
                mOnSeekCompleteListener.onSeekComplete(mp);
            }
        }
    };

    private MediaPlayer.OnCompletionListener mCompletionListener =
            new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            if(mOnCompletionListener != null) {
                mOnCompletionListener.onCompletion(mp);
            }
        }
    };

    private MediaPlayer.OnErrorListener mErrorListener =
            new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            if(mOnErrorListener != null) {
                return mOnErrorListener.onError(mp, what, extra);
            }

            Toast.makeText(getContext(), "Cannot play the video", Toast.LENGTH_LONG).show();

            return true;
        }
    };

    private MediaPlayer.OnInfoListener mInfoListener =
            new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            if(mOnInfoListener != null) {
                return mOnInfoListener.onInfo(mp, what, extra);
            }
            return true;
        }
    };

    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
            new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if(mOnBufferingUpdateListener != null) {
                mOnBufferingUpdateListener.onBufferingUpdate(mp, percent);
            }
        }
    };
}
