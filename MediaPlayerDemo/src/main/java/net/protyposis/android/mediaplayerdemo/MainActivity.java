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
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.protyposis.android.mediaplayer.MediaSource;
import net.protyposis.android.mediaplayer.dash.DashSource;

public class MainActivity extends Activity implements VideoURIInputDialogFragment.OnVideoURISelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_LOAD_VIDEO = 1;

    private Button mVideoSelectButton;
    private Button mVideoSelect2Button;
    private Button mVideoViewButton;
    private Button mSideBySideButton;
    private Button mSideBySideSeekTestButton;

    private TextView mVideoUriText;
    private int mVideoUriTextColor;
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
        mSideBySideButton = (Button) findViewById(R.id.sidebyside);
        mSideBySideSeekTestButton = (Button) findViewById(R.id.sidebysideseektest);
        mVideoUriText = (TextView) findViewById(R.id.videouri);
        mVideoUriTextColor = mVideoUriText.getCurrentTextColor();

        mVideoSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // open the picker...
                Log.d(TAG, "opening video picker...");
                Intent intent = null;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("video/*");
                } else {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"video/*", "audio/*"});
                }
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
        if(!updateUri(uri)) {
            Toast.makeText(this, "Invalid media URL", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("uri", mVideoUri);
        super.onSaveInstanceState(outState);
    }

    private boolean updateUri(final Uri uri) {
        if(uri == null) {
            mVideoUriText.setText(getString(R.string.uri_missing));

            mVideoViewButton.setEnabled(false);
            mSideBySideButton.setEnabled(false);
            mSideBySideSeekTestButton.setEnabled(false);
        } else {
            updateUri(null); // disable buttons

            // Validate content URI
            try {
                if(uri.getScheme().equals("content")) {
                    ContentResolver cr = getContentResolver();
                    cr.openInputStream(uri).close();
                }
            } catch (Exception e) {
                // The content URI is invalid, probably because the file has been removed
                // or the system rebooted (which invalidates content URIs),
                // or the uri does not contain a scheme
                return false;
            }


            mVideoUriText.setText("Loading...");

            Utils.uriToMediaSourceAsync(MainActivity.this, uri, new Utils.MediaSourceAsyncCallbackHandler() {
                @Override
                public void onMediaSourceLoaded(MediaSource mediaSource) {
                    String text = uri.toString();
                    if (mediaSource instanceof DashSource) {
                        text = "DASH: " + text;
                    }
                    mVideoUriText.setText(text);
                    mVideoUriText.setTextColor(mVideoUriTextColor);
                    mVideoUri = uri;

                    mVideoViewButton.setEnabled(true);
                    mSideBySideButton.setEnabled(!(mediaSource instanceof DashSource));
                    mSideBySideSeekTestButton.setEnabled(true);

                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                            .edit().putString("lastUri", uri.toString()).commit();
                }

                @Override
                public void onException(Exception e) {
                    mVideoUriText.setText("Error loading video" + (e.getMessage() != null ? ": " + e.getMessage() : " :("));
                    mVideoUriText.setTextColor(Color.RED);
                    Log.e(TAG, "Error loading video", e);
                }
            });
        }

        return true;
    }

    private void versionInfos() {
        String versionInfos = "";
        Map<String, Class> components = new LinkedHashMap<String, Class>();
        components.put("MediaPlayer", net.protyposis.android.mediaplayer.BuildConfig.class);
        components.put("MediaPlayer-DASH", net.protyposis.android.mediaplayer.dash.BuildConfig.class);
        components.put("MediaPlayerDemo", net.protyposis.android.mediaplayerdemo.BuildConfig.class);

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
