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
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import at.aau.itec.android.mediaplayer.MediaSource;
import at.aau.itec.android.mediaplayer.dash.DashSource;

public class MainActivity extends Activity implements VideoURIInputDialogFragment.OnVideoURISelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_LOAD_VIDEO = 1;

    private Button mVideoSelectButton;
    private Button mVideoSelect2Button;
    private Button mVideoViewButton;
    private Button mGLVideoViewButton;
    private Button mGLCameraViewButton;
    private Button mSideBySideButton;
    private Button mSideBySideSeekTestButton;

    private TextView mVideoUriText;
    private Uri mVideoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(BuildConfig.CRASHLYTICS_CONFIGURED) {
            Fabric.with(this, new Crashlytics());
        } else {
            Log.w(TAG, "Crashlytics not configured!");
        }

        setContentView(R.layout.activity_main);

        mVideoSelectButton = (Button) findViewById(R.id.videoselect);
        mVideoSelect2Button = (Button) findViewById(R.id.videoselect2);
        mVideoViewButton = (Button) findViewById(R.id.videoview);
        mGLVideoViewButton = (Button) findViewById(R.id.glvideoview);
        mGLCameraViewButton = (Button) findViewById(R.id.glcameraview);
        mSideBySideButton = (Button) findViewById(R.id.sidebyside);
        mSideBySideSeekTestButton = (Button) findViewById(R.id.sidebysideseektest);
        mVideoUriText = (TextView) findViewById(R.id.videouri);

        mVideoSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // open the picker...
                Log.d(TAG, "opening video picker...");
                Intent intent = null;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    intent = new Intent(Intent.ACTION_PICK);
                } else {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                }
                intent.setType("video/*");
                startActivityForResult(intent, REQUEST_LOAD_VIDEO);
            }
        });
        mVideoSelect2Button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    VideoURIInputDialogFragment dialog = new VideoURIInputDialogFragment();
                    dialog.show(getFragmentManager(), null);
                }
        });

        mVideoViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, VideoViewActivity.class).setData(mVideoUri));
            }
        });
        mGLVideoViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, GLVideoViewActivity.class).setData(mVideoUri));
            }
        });
        mGLCameraViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, GLCameraViewActivity.class).setData(mVideoUri));
            }
        });
        mSideBySideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SideBySideActivity.class).setData(mVideoUri));
            }
        });
        mSideBySideSeekTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SideBySideSeekTestActivity.class).setData(mVideoUri));
            }
        });
        ((Button) findViewById(R.id.licenses)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebView licensesWebView = new WebView(MainActivity.this);
                licensesWebView.loadUrl("file:///android_asset/licenses.html");

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.open_source_licenses))
                        .setView(licensesWebView)
                        .create()
                        .show();
            }
        });

        Uri uri = null;

        if (getIntent().getData() != null) {
            // The intent-filter probably caught an url, open it...
            uri = getIntent().getData();
        } else {
            String savedUriString = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .getString("lastUri", "");
            if(!"".equals(savedUriString)) {
                uri = Uri.parse(savedUriString);
            }
        }

        // internet streaming test files
        //uri = Uri.parse("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");
        //uri = Uri.parse("http://www-itec.uni-klu.ac.at/dash/js/content/bunny_4000.webm");

        // internet DASH streaming test files
        //uri = Uri.parse("http://www-itec.uni-klu.ac.at/dash/js/content/bigbuckbunny_1080p.mpd");
        //uri = Uri.parse("http://www-itec.uni-klu.ac.at/dash/js/content/bunny_ibmff_1080.mpd");
        //uri = Uri.parse("http://dj9wk94416cg5.cloudfront.net/sintel2/sintel.mpd");

        if(savedInstanceState != null) {
            uri = savedInstanceState.getParcelable("uri");
        }

        updateUri(uri);
        versionInfos();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_LOAD_VIDEO) {
            Log.d(TAG, "onActivityResult REQUEST_LOAD_VIDEO");

            if(resultCode == RESULT_OK) {
                updateUri(data.getData());
            } else {
                Log.w(TAG, "no file specified");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onVideoURISelected(Uri uri) {
        updateUri(uri);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("uri", mVideoUri);
        super.onSaveInstanceState(outState);
    }

    private void updateUri(Uri uri) {
        if(uri == null) {
            mVideoUriText.setText(getString(R.string.uri_missing));

            mVideoViewButton.setEnabled(false);
            mGLVideoViewButton.setEnabled(false);
            mSideBySideButton.setEnabled(false);
            mSideBySideSeekTestButton.setEnabled(false);
        } else {
            String text = uri.toString();
            MediaSource mediaSource = Utils.uriToMediaSource(this, uri);
            if(mediaSource instanceof DashSource) {
                text = "DASH: " + text;
            }
            mVideoUriText.setText(text);
            mVideoUri = uri;

            mVideoViewButton.setEnabled(true);
            mGLVideoViewButton.setEnabled(true);
            mSideBySideButton.setEnabled(!(mediaSource instanceof DashSource));
            mSideBySideSeekTestButton.setEnabled(true);

            PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .edit().putString("lastUri", uri.toString()).commit();
        }
    }

    private void versionInfos() {
        String versionInfos = "";
        Map<String, Class> components = new LinkedHashMap<String, Class>();
        components.put("MediaPlayer", at.aau.itec.android.mediaplayer.BuildConfig.class);
        components.put("MediaPlayer-DASH", at.aau.itec.android.mediaplayer.dash.BuildConfig.class);
        components.put("MediaPlayer-GLES", at.aau.itec.android.mediaplayer.gles.BuildConfig.class);
        components.put("MediaPlayer-GLES-FlowAbs", at.aau.itec.android.mediaplayer.gles.flowabs.BuildConfig.class);
        components.put("MediaPlayer-GLES-QrMarker", at.aau.itec.android.mediaplayer.gles.qrmarker.BuildConfig.class);
        components.put("MediaPlayerDemo", at.aau.itec.android.mediaplayerdemo.BuildConfig.class);

        Iterator<String> componentIterator = components.keySet().iterator();
        while(componentIterator.hasNext()) {
            String component = componentIterator.next();
            versionInfos += component + ":" + versionInfo(components.get(component));
            if(componentIterator.hasNext()) {
                versionInfos += ", ";
            }
        }

        ((TextView) findViewById(R.id.versioninfos)).setText(versionInfos);
    }

    private String versionInfo(Class buildInfo) {
        String info = "";
        try {
            info += buildInfo.getField("VERSION_NAME").get(null).toString();
            info += "/";
            info += buildInfo.getField("VERSION_CODE").get(null).toString();
            info += "/";
            info += buildInfo.getField("BUILD_TYPE").get(null).toString();
            info += buildInfo.getField("FLAVOR").get(null).toString();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return info.length() == 0 ? "n/a" : info;
    }
}
