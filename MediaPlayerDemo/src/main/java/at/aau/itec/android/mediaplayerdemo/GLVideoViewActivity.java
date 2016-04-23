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

package at.aau.itec.android.mediaplayerdemo;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;

import at.aau.itec.android.mediaplayer.GLVideoView;
import at.aau.itec.android.mediaplayer.MediaPlayer;
import at.aau.itec.android.mediaplayer.MediaSource;


public class GLVideoViewActivity extends Activity {

    private static final String TAG = GLVideoViewActivity.class.getSimpleName();

    private Uri mVideoUri;
    private GLVideoView mGLVideoView;
    private ProgressBar mProgress;

    private MediaController.MediaPlayerControl mMediaPlayerControl;
    private MediaController mMediaController;

    private GLEffects mEffectList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glvideoview); // reuse main layout
        Utils.setActionBarSubtitleEllipsizeMiddle(this);

        mGLVideoView = (GLVideoView) findViewById(R.id.glvv);
        mProgress = (ProgressBar) findViewById(R.id.progress);

        mMediaPlayerControl = mGLVideoView;
        mMediaController = new MediaController(this);
        mMediaController.setAnchorView(findViewById(R.id.container));
        mMediaController.setMediaPlayer(mMediaPlayerControl);
        mMediaController.setEnabled(false);

        mProgress.setVisibility(View.VISIBLE);

        mEffectList = new GLEffects(this, R.id.parameterlist, mGLVideoView);
        mEffectList.addEffects();

        if(savedInstanceState != null) {
            initPlayer((Uri)savedInstanceState.getParcelable("uri"),
                    savedInstanceState.getInt("position"),
                    savedInstanceState.getBoolean("playing"));
        } else {
            initPlayer(getIntent().getData(), -1, false);
        }
    }

    private void initPlayer(Uri uri, final int position, final boolean playback) {
        mVideoUri = uri;
        getActionBar().setSubtitle(mVideoUri+"");

        mGLVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer vp) {
                if (position > 0) {
                    mGLVideoView.seekTo(position);
                } else {
                    mGLVideoView.seekTo(0); // display first frame
                }

                if (playback) {
                    mGLVideoView.start();
                }

                mProgress.setVisibility(View.GONE);
                mMediaController.setEnabled(true);
            }
        });
        mGLVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(GLVideoViewActivity.this,
                        "Cannot play the video, see logcat for the detailed exception",
                        Toast.LENGTH_LONG).show();
                mProgress.setVisibility(View.GONE);
                mMediaController.setEnabled(false);
                return true;
            }
        });
        mGLVideoView.setOnSeekListener(new MediaPlayer.OnSeekListener() {
            @Override
            public void onSeek(MediaPlayer mp) {
                mProgress.setVisibility(View.VISIBLE);
            }
        });
        mGLVideoView.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                mProgress.setVisibility(View.GONE);
            }
        });
        mGLVideoView.setOnFrameCapturedCallback(new Utils.OnFrameCapturedCallback(this, "glvideoview"));

        Utils.uriToMediaSourceAsync(this, uri, new Utils.MediaSourceAsyncCallbackHandler() {
            @Override
            public void onMediaSourceLoaded(MediaSource mediaSource) {
                mGLVideoView.setVideoSource(mediaSource);
            }

            @Override
            public void onException(Exception e) {
                Log.e(TAG, "error loading video", e);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.glvideoview, menu);
        getMenuInflater().inflate(R.menu.videoview, menu);
        mEffectList.addToMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_slowspeed) {
            mGLVideoView.setPlaybackSpeed(0.2f);
            return true;
        } else if(id == R.id.action_halfspeed) {
            mGLVideoView.setPlaybackSpeed(0.5f);
            return true;
        } else if(id == R.id.action_doublespeed) {
            mGLVideoView.setPlaybackSpeed(2.0f);
            return true;
        } else if(id == R.id.action_quadspeed) {
            mGLVideoView.setPlaybackSpeed(4.0f);
            return true;
        } else if(id == R.id.action_normalspeed) {
            mGLVideoView.setPlaybackSpeed(1.0f);
            return true;
        } else if(id == R.id.action_seekcurrentposition) {
            mGLVideoView.pause();
            mGLVideoView.seekTo(mGLVideoView.getCurrentPosition());
            return true;
        } else if(id == R.id.action_seekcurrentpositionplus1ms) {
            mGLVideoView.pause();
            mGLVideoView.seekTo(mGLVideoView.getCurrentPosition()+1);
            return true;
        } else if(id == R.id.action_seektoend) {
            mGLVideoView.pause();
            mGLVideoView.seekTo(mGLVideoView.getDuration());
            return true;
        } else if(id == R.id.action_getcurrentposition) {
            Toast.makeText(this, "current position: " + mGLVideoView.getCurrentPosition(), Toast.LENGTH_SHORT).show();
            return true;
        } else if(mEffectList.doMenuActions(item)) {
            return true;
        } else if(id == R.id.action_save_frame) {
            mGLVideoView.captureFrame();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            long durationMs = event.getEventTime() - event.getDownTime();
            /* The media controller is only getting toggled by simple taps.  If a certain amount of
             * time passes between the DOWN and UP actions, it can be considered as not being a
             * simple tap any more and the media controller is not getting toggled.
             */
            if(durationMs < 500) {
                if (mMediaController.isShowing()) {
                    mMediaController.hide();
                } else {
                    mMediaController.show();
                }
            }
        }

        // hand the event to the video view to process zoom/pan gestures
        mGLVideoView.onTouchEvent(event);

        return super.onTouchEvent(event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLVideoView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLVideoView.onResume();
    }

    @Override
    protected void onStop() {
        mMediaController.hide();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mVideoUri != null) {
            outState.putParcelable("uri", mVideoUri);
            outState.putBoolean("playing", mGLVideoView.isPlaying());
            outState.putInt("position", mGLVideoView.getCurrentPosition());
        }
    }
}
