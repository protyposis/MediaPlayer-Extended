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
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import net.protyposis.android.mediaplayer.MediaSource;
import net.protyposis.android.mediaplayer.UriSource;
import net.protyposis.android.mediaplayer.dash.AdaptationLogic;
import net.protyposis.android.mediaplayer.dash.DashSource;
import net.protyposis.android.mediaplayer.dash.SimpleRateBasedAdaptationLogic;

/**
 * Created by maguggen on 28.08.2014.
 */
public class Utils {

    private static final String TAG = Utils.class.getSimpleName();

    public static MediaSource uriToMediaSource(Context context, Uri uri) {
        MediaSource source = null;

        // A DASH source is either detected if the given URL has an .mpd extension or if the DASH
        // pseudo protocol has been prepended.
        if(uri.toString().endsWith(".mpd") || uri.toString().startsWith("dash://")) {
            AdaptationLogic adaptationLogic;

            // Strip dash:// pseudo protocol
            if(uri.toString().startsWith("dash://")) {
                uri = Uri.parse(uri.toString().substring(7));
            }

            //adaptationLogic = new ConstantPropertyBasedLogic(ConstantPropertyBasedLogic.Mode.HIGHEST_BITRATE);
            adaptationLogic = new SimpleRateBasedAdaptationLogic();

            source = new DashSource(context, uri, adaptationLogic);
        } else {
            source = new UriSource(context, uri);
        }
        return source;
    }

    public static void uriToMediaSourceAsync(final Context context, Uri uri, MediaSourceAsyncCallbackHandler callback) {
        LoadMediaSourceAsyncTask loadingTask = new LoadMediaSourceAsyncTask(context, callback);

        try {
            loadingTask.execute(uri).get();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public static void setActionBarSubtitleEllipsizeMiddle(Activity activity) {
        // http://blog.wu-man.com/2011/12/actionbar-api-provided-by-google-on.html
        int subtitleId = activity.getResources().getIdentifier("action_bar_subtitle", "id", "android");
        TextView subtitleView = (TextView) activity.findViewById(subtitleId);
        subtitleView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
    }

    public static boolean saveBitmapToFile(Bitmap bmp, File file) {
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
            bos.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "failed to save frame", e);
        }
        return false;
    }

    private static class LoadMediaSourceAsyncTask extends AsyncTask<Uri, Void, MediaSource> {

        private Context mContext;
        private MediaSourceAsyncCallbackHandler mCallbackHandler;
        private MediaSource mMediaSource;
        private Exception mException;

        public LoadMediaSourceAsyncTask(Context context, MediaSourceAsyncCallbackHandler callbackHandler) {
            mContext = context;
            mCallbackHandler = callbackHandler;
        }

        @Override
        protected MediaSource doInBackground(Uri... params) {
            try {
                mMediaSource = Utils.uriToMediaSource(mContext, params[0]);
                return mMediaSource;
            } catch (Exception e) {
                mException = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(MediaSource mediaSource) {
            if(mException != null) {
                mCallbackHandler.onException(mException);
            } else {
                mCallbackHandler.onMediaSourceLoaded(mMediaSource);
            }
        }
    }

    public static interface MediaSourceAsyncCallbackHandler {
        void onMediaSourceLoaded(MediaSource mediaSource);
        void onException(Exception e);
    }

    /**
     * Iterates a hierarchy of exceptions and combines their messages. Useful for compact
     * error representation to the user during debugging/development.
     */
    public static String getExceptionMessageHistory(Throwable e) {
        String messages = "";

        String message = e.getMessage();
        if(message != null) {
            messages += message;
        }
        while((e = e.getCause()) != null) {
            message = e.getMessage();
            if(message != null) {
                messages += " <- " + message;
            }
        }

        return messages;
    }
}
