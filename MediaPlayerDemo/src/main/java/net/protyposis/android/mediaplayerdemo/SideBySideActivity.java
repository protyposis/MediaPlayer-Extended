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

import android.annotation.TargetApi;
import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.MediaController;

import java.util.ArrayList;
import java.util.List;

import net.protyposis.android.mediaplayer.VideoView;


public class SideBySideActivity extends Activity {

    private static final String TAG = SideBySideActivity.class.getSimpleName();

    private Uri mVideoUri;
    private android.widget.VideoView mAndroidVideoView;
    private VideoView mMpxVideoView;

    private MediaController.MediaPlayerControl mMediaPlayerControl;
    private MediaController mMediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_side_by_side);
        Utils.setActionBarSubtitleEllipsizeMiddle(this);

        mAndroidVideoView = (android.widget.VideoView) findViewById(R.id.androidvv);
        mMpxVideoView = (VideoView) findViewById(R.id.mpxvv);

        mMediaPlayerControl = new MediaPlayerMultiControl(mAndroidVideoView, mMpxVideoView);
        mMediaController = new MediaController(this);
        mMediaController.setAnchorView(findViewById(R.id.container));
        mMediaController.setMediaPlayer(mMediaPlayerControl);

        mVideoUri = getIntent().getData();
        getActionBar().setSubtitle(mVideoUri+"");

        // HACK: this needs to be deferred, else it fails when setting video on both players (it works when doing it just on one)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mAndroidVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mAndroidVideoView.seekTo(0); // display first frame
                    }
                });
                mMpxVideoView.setOnPreparedListener(new net.protyposis.android.mediaplayer.MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(net.protyposis.android.mediaplayer.MediaPlayer mp) {
                        mMpxVideoView.seekTo(0); // display first frame
                    }
                });

                mAndroidVideoView.setVideoURI(mVideoUri);
                mMpxVideoView.setVideoURI(mVideoUri);
            }
        }, 1000);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.side_by_side, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mMediaController.show();
        return super.onTouchEvent(event);
    }

    @Override
    protected void onStop() {
        mMediaController.hide();
        super.onStop();
    }

    private class MediaPlayerMultiControl implements MediaController.MediaPlayerControl {

        private List<MediaController.MediaPlayerControl> mControls;

        public MediaPlayerMultiControl(MediaController.MediaPlayerControl... controls) {
            mControls = new ArrayList<MediaController.MediaPlayerControl>();
            for(MediaController.MediaPlayerControl mpc : controls) {
                mControls.add(mpc);
            }
        }

        @Override
        public void start() {
            for(MediaController.MediaPlayerControl mpc : mControls) {
                mpc.start();
            }
        }

        @Override
        public void pause() {
            for(MediaController.MediaPlayerControl mpc : mControls) {
                mpc.pause();
            }
        }

        @Override
        public int getDuration() {
            if(!mControls.isEmpty()) {
                return mControls.get(0).getDuration();
            }
            return 0;
        }

        @Override
        public int getCurrentPosition() {
            if(!mControls.isEmpty()) {
                return mControls.get(0).getCurrentPosition();
            }
            return 0;
        }

        @Override
        public void seekTo(int pos) {
            for(MediaController.MediaPlayerControl mpc : mControls) {
                mpc.seekTo(pos);
            }
        }

        @Override
        public boolean isPlaying() {
            if(!mControls.isEmpty()) {
                return mControls.get(0).isPlaying();
            }
            return false;
        }

        @Override
        public int getBufferPercentage() {
            if(!mControls.isEmpty()) {
                return mControls.get(0).getBufferPercentage();
            }
            return 0;
        }

        @Override
        public boolean canPause() {
            if(!mControls.isEmpty()) {
                return mControls.get(0).canPause();
            }
            return false;
        }

        @Override
        public boolean canSeekBackward() {
            if(!mControls.isEmpty()) {
                return mControls.get(0).canSeekBackward();
            }
            return false;
        }

        @Override
        public boolean canSeekForward() {
            if(!mControls.isEmpty()) {
                return mControls.get(0).canSeekForward();
            }
            return false;
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public int getAudioSessionId() {
            if(!mControls.isEmpty()) {
                return mControls.get(0).getAudioSessionId();
            }
            return 0;
        }
    }
}
