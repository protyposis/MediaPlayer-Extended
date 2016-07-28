/*
 * Copyright (c) 2014 Mario Guggenberger <mg@protyposis.net>
 *
 * This file is part of MediaPlayer-Extended.
 *
 * MediaPlayer-Extended is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MediaPlayer-Extended is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MediaPlayer-Extended.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.protyposis.android.mediaplayerdemo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.MediaController;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.protyposis.android.mediaplayer.MediaPlayer;
import net.protyposis.android.mediaplayer.MediaSource;
import net.protyposis.android.mediaplayer.VideoView;


public class SideBySideSeekTestActivity extends Activity {

    private static final String TAG = SideBySideSeekTestActivity.class.getSimpleName();

    private VideoView mVideoView1;
    private VideoView mVideoView2;

    private Spinner mSpinner1;
    private Spinner mSpinner2;

    private MediaController.MediaPlayerControl mMediaPlayerControl;
    private MediaController mMediaController;
    private int mSeekTarget;
    private int mSeekingViewsCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_side_by_side_seektest);
        Utils.setActionBarSubtitleEllipsizeMiddle(this);

        mVideoView1 = (VideoView) findViewById(R.id.vv1);
        mVideoView2 = (VideoView) findViewById(R.id.vv2);
        mSpinner1 = (Spinner) findViewById(R.id.vv1_mode);
        mSpinner2 = (Spinner) findViewById(R.id.vv2_mode);

        mMediaPlayerControl = new MediaPlayerMultiControl(mVideoView1, mVideoView2);
        mMediaController = new MediaController(this);
        mMediaController.setAnchorView(findViewById(R.id.container));
        mMediaController.setMediaPlayer(mMediaPlayerControl);

        final Uri uri = getIntent().getData();
        getActionBar().setSubtitle(uri+"");

        final Map<VideoView, Spinner> videoViews = new LinkedHashMap<VideoView, Spinner>(2);
        videoViews.put(mVideoView1, mSpinner1);
        videoViews.put(mVideoView2, mSpinner2);

        final int[] count = {0};
        for(final VideoView videoView : videoViews.keySet()) {
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Spinner spinner = videoViews.get(videoView);
                    ArrayAdapter<MediaPlayer.SeekMode> dataAdapter = new ArrayAdapter<MediaPlayer.SeekMode>(
                            SideBySideSeekTestActivity.this,
                            android.R.layout.simple_spinner_item,
                            MediaPlayer.SeekMode.values()
                    );
                    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(dataAdapter);
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            // set seek mode
                            videoView.getMediaPlayer().setSeekMode((MediaPlayer.SeekMode) parent.getItemAtPosition(position));
                            // update picture
                            mSeekingViewsCount++;
                            setProgressBarIndeterminateVisibility(true);
                            videoView.seekTo(mSeekTarget);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                    if(++count[0] == videoViews.size()) {
                        mSpinner1.setSelection(0);
                        mSpinner2.setSelection(5);
                    }
                }
            });
            videoView.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    if(--mSeekingViewsCount == 0) {
                        setProgressBarIndeterminateVisibility(false);
                    }
                }
            });

            Utils.uriToMediaSourceAsync(this, uri, new Utils.MediaSourceAsyncCallbackHandler() {
                @Override
                public void onMediaSourceLoaded(MediaSource mediaSource) {
                    videoView.setVideoSource(mediaSource);
                }

                @Override
                public void onException(Exception e) {
                    Log.e(TAG, "error loading video", e);
                }
            });
        }
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
            mSeekTarget = pos;
            mSeekingViewsCount = mControls.size();
            setProgressBarIndeterminateVisibility(true);
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
