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

package net.protyposis.android.mediaplayerdemo;

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

import net.protyposis.android.mediaplayer.Cue;
import net.protyposis.android.mediaplayer.MediaPlayer;
import net.protyposis.android.mediaplayer.MediaSource;
import net.protyposis.android.mediaplayer.VideoView;

public class VideoViewActivity extends Activity {

    private static final String TAG = VideoViewActivity.class.getSimpleName();

    private VideoView mVideoView;
    private ProgressBar mProgress;

    private MediaController.MediaPlayerControl mMediaPlayerControl;
    private MediaController mMediaController;

    private Uri mVideoUri;
    private int mVideoPosition;
    private float mVideoPlaybackSpeed;
    private boolean mVideoPlaying;
    private MediaSource mMediaSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videoview);
        Utils.setActionBarSubtitleEllipsizeMiddle(this);

        mVideoView = (VideoView) findViewById(R.id.vv);
        mProgress = (ProgressBar) findViewById(R.id.progress);

        mMediaPlayerControl = mVideoView; //new MediaPlayerDummyControl();
        mMediaController = new MediaController(this);
        mMediaController.setAnchorView(findViewById(R.id.container));
        mMediaController.setMediaPlayer(mMediaPlayerControl);
        mMediaController.setEnabled(false);

        mProgress.setVisibility(View.VISIBLE);

        // Init video playback state (will eventually be overwritten by saved instance state)
        mVideoUri = getIntent().getData();
        mVideoPosition = 0;
        mVideoPlaybackSpeed = 1;
        mVideoPlaying = false;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mVideoUri = savedInstanceState.getParcelable("uri");
        mVideoPosition = savedInstanceState.getInt("position");
        mVideoPlaybackSpeed = savedInstanceState.getFloat("playbackSpeed");
        mVideoPlaying = savedInstanceState.getBoolean("playing");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!mVideoView.isPlaying()) {
            initPlayer();
        }
    }

    private void initPlayer() {
        getActionBar().setSubtitle(mVideoUri+"");

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer vp) {
                mProgress.setVisibility(View.GONE);
                mMediaController.setEnabled(true);

                vp.addCue(1000, "test cue @ 1000");
                vp.addCue(2000, "test cue @ 2000");
                vp.addCue(3000, "test cue @ 3000");
                vp.addCue(10000, "test cue @ 10000");
                vp.setOnCueListener(new MediaPlayer.OnCueListener() {
                    @Override
                    public void onCue(MediaPlayer mp, Cue cue) {
                        Log.d(TAG, "onCue: " + cue.toString() + " (" + mp.getCurrentPosition() + ")");
                    }
                });
            }
        });
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(VideoViewActivity.this,
                        "Cannot play the video, see logcat for the detailed exception",
                        Toast.LENGTH_LONG).show();
                mProgress.setVisibility(View.GONE);
                mMediaController.setEnabled(false);
                return true;
            }
        });
        mVideoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                String whatName = "";
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        whatName = "MEDIA_INFO_BUFFERING_END";
                        break;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        whatName = "MEDIA_INFO_BUFFERING_START";
                        break;
                    case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                        whatName = "MEDIA_INFO_VIDEO_RENDERING_START";
                        break;
                    case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                        whatName = "MEDIA_INFO_VIDEO_TRACK_LAGGING";
                        break;
                }
                Log.d(TAG, "onInfo " + whatName);
                return false;
            }
        });
        mVideoView.setOnSeekListener(new MediaPlayer.OnSeekListener() {
            @Override
            public void onSeek(MediaPlayer mp) {
                Log.d(TAG, "onSeek");
                mProgress.setVisibility(View.VISIBLE);
            }
        });
        mVideoView.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                Log.d(TAG, "onSeekComplete");
                mProgress.setVisibility(View.GONE);
            }
        });
        mVideoView.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                Log.d(TAG, "onBufferingUpdate " + percent + "%");
            }
        });

        Utils.MediaSourceAsyncCallbackHandler mMediaSourceAsyncCallbackHandler =
                new Utils.MediaSourceAsyncCallbackHandler() {
            @Override
            public void onMediaSourceLoaded(MediaSource mediaSource) {
                mMediaSource = mediaSource;
                mVideoView.setVideoSource(mediaSource);
                mVideoView.seekTo(mVideoPosition);
                mVideoView.setPlaybackSpeed(mVideoPlaybackSpeed);
                if (mVideoPlaying) {
                    mVideoView.start();
                }
            }

            @Override
            public void onException(Exception e) {
                Log.e(TAG, "error loading video", e);
            }
        };
        if(mMediaSource == null) {
            // Convert uri to media source asynchronously to avoid UI blocking
            // It could take a while, e.g. if it's a DASH source and needs to be preprocessed
            Utils.uriToMediaSourceAsync(this, mVideoUri, mMediaSourceAsyncCallbackHandler);
        } else {
            // Media source is already here, just use it
            mMediaSourceAsyncCallbackHandler.onMediaSourceLoaded(mMediaSource);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.videoview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_slowspeed) {
            mVideoView.setPlaybackSpeed(0.2f);
            return true;
        } else if(id == R.id.action_halfspeed) {
            mVideoView.setPlaybackSpeed(0.5f);
            return true;
        } else if(id == R.id.action_doublespeed) {
            mVideoView.setPlaybackSpeed(2.0f);
            return true;
        } else if(id == R.id.action_quadspeed) {
            mVideoView.setPlaybackSpeed(4.0f);
            return true;
        } else if(id == R.id.action_normalspeed) {
            mVideoView.setPlaybackSpeed(1.0f);
            return true;
        } else if(id == R.id.action_seekcurrentposition) {
            mVideoView.pause();
            mVideoView.seekTo(mVideoView.getCurrentPosition());
            return true;
        } else if(id == R.id.action_seekcurrentpositionplus1ms) {
            mVideoView.pause();
            mVideoView.seekTo(mVideoView.getCurrentPosition()+1);
            return true;
        } else if(id == R.id.action_seektoend) {
            mVideoView.pause();
            mVideoView.seekTo(mVideoView.getDuration());
            return true;
        } else if(id == R.id.action_getcurrentposition) {
            Toast.makeText(this, "current position: " + mVideoView.getCurrentPosition(), Toast.LENGTH_SHORT).show();
            return true;
        } else if(id == R.id.action_reload_source) {
            initPlayer();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mMediaController.isShowing()) {
                mMediaController.hide();
            } else {
                mMediaController.show();
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onStop() {
        mMediaController.hide();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mVideoView != null) {
            mVideoPosition = mVideoView.getCurrentPosition();
            mVideoPlaybackSpeed = mVideoView.getPlaybackSpeed();
            mVideoPlaying = mVideoView.isPlaying();
            // the uri is stored in the base activity
            outState.putParcelable("uri", mVideoUri);
            outState.putInt("position", mVideoPosition);
            outState.putFloat("playbackSpeed", mVideoView.getPlaybackSpeed());
            outState.putBoolean("playing", mVideoPlaying);
        }
    }
}
