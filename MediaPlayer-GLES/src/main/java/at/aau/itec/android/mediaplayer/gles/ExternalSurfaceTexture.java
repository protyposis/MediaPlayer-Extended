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

package at.aau.itec.android.mediaplayer.gles;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.view.Surface;

/**
 * @author Mario Guggenberger
 */
public class ExternalSurfaceTexture extends Texture implements SurfaceTexture.OnFrameAvailableListener {

    private static final long NANOTIME_SECOND = 1000000000;

    private SurfaceTexture mSurfaceTexture;
    private SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener;
    private boolean mFrameAvailable;

    public ExternalSurfaceTexture() {
        super();

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        mTexture = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexture);
        GLUtils.checkError("glBindTexture");

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.checkError("glTexParameter");

        // This surface texture needs to be fed to the media player; through it,
        // the picture data will be written into the texture.
        mSurfaceTexture = new SurfaceTexture(mTexture);
        mSurfaceTexture.setOnFrameAvailableListener(this);
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public Surface getSurface() {
        return new Surface(mSurfaceTexture);
    }

    public void setOnFrameAvailableListener(SurfaceTexture.OnFrameAvailableListener l) {
        mOnFrameAvailableListener = l;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        notifyFrameAvailability();
    }

    /**
     * THIS DOES NOT WORK.
     * This event handler does not execute in the same thread from which {@link android.media.MediaCodec#releaseOutputBuffer(int, long)}
     * is called (i.e. the MediaPlayer playback thread), so sleeping here does not block the
     * releaseOutputBuffer call like it should and like it does when rendering to a SurfaceView.
     *
     * Things already tried:
     *  - Handler with sendEmptyMessageDelayed (does not block at all, so it is the wrong approach)
     *  - Thread.sleep()
     * Both cannot work since this is the wrong thread.
     *
     * TODO find a way to block releaseOutputBuffer when rendering to a GL texture
     * @param surfaceTexture
     */
    private void onFrameAvailableWithTimestampDelayHandling(SurfaceTexture surfaceTexture) {
        /**
         * Handle the associated timestamp of the video frame texture. There are 2 cases we
         * need to take care of:
         *
         * 1. A call to {@link android.media.MediaCodec#releaseOutputBuffer(int, boolean)} delivers
         *    a zero timestamp (0), and rendering must take place immediately.
         *
         * 2. A call to {@link android.media.MediaCodec#releaseOutputBuffer(int, long)} delivers
         *    a nanosecond timestamp close to {@link System#nanoTime}, and rendering must be deferred
         *    until the specified time. Rendering will only be deferred if the timestamp is within
         *    one (1) second, and documented at the MediaCodec method, to stay compatible with the
         *    Android API. In all other cases, the frame will be rendered immediately.
         */
        long timestamp = surfaceTexture.getTimestamp();

        if (timestamp == 0) {
            // render immediately
            notifyFrameAvailability();
        } else {
            long delay = timestamp - System.nanoTime();

            if (delay > 0 && delay < NANOTIME_SECOND) {
                // The timestamp lies within the near future, defer rendering until that time
                try {
                    Thread.sleep(delay / 1000000, (int)(delay % 1000000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                notifyFrameAvailability();
            } else {
                // "Invalid" timestamp, also render immediately
                notifyFrameAvailability();
            }
        }
    }

    private void notifyFrameAvailability() {
        mFrameAvailable = true;
        if(mOnFrameAvailableListener != null) {
            mOnFrameAvailableListener.onFrameAvailable(mSurfaceTexture);
        }
    }

    public boolean isTextureUpdateAvailable() {
        return mFrameAvailable;
    }

    public void updateTexture() {
        mFrameAvailable = false;
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mTransformMatrix);
    }
}
