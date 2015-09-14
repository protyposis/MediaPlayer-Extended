/*
 * Copyright (c) 2014 Mario Guggenberger <mg@itec.aau.at>
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
import android.content.Intent;
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

import at.aau.itec.android.mediaplayer.MediaPlayer;
import at.aau.itec.android.mediaplayer.MediaSource;
import at.aau.itec.android.mediaplayer.VideoView;

public class VideoViewActivity extends Activity {

    private static final String TAG = VideoViewActivity.class.getSimpleName();

    private Uri mVideoUri;
    private VideoView mVideoView;
    private ProgressBar mProgress;

    private MediaController.MediaPlayerControl mMediaPlayerControl;
    private MediaController mMediaController;

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

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer vp) {
                if (position > 0) {
                    mVideoView.seekTo(position);
                } else {
                    mVideoView.seekTo(0); // display first frame
                }

                if (playback) {
                    mVideoView.start();
                }

                mProgress.setVisibility(View.GONE);
                mMediaController.setEnabled(true);
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

        Utils.uriToMediaSourceAsync(this, uri, new Utils.MediaSourceAsyncCallbackHandler() {
            @Override
            public void onMediaSourceLoaded(MediaSource mediaSource) {
                mVideoView.setVideoSource(mediaSource);
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
        if(mVideoUri != null) {
            outState.putParcelable("uri", mVideoUri);
            outState.putBoolean("playing", mVideoView.isPlaying());
            outState.putInt("position", mVideoView.getCurrentPosition());
        }
    }
}
