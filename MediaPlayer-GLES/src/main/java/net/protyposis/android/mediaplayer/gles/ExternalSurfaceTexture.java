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

package net.protyposis.android.mediaplayer.gles;

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
        /**
         * Timed rendering through {@link android.media.MediaCodec#releaseOutputBuffer(int, long)}
         * does not work with this texture. When a frame becomes available here, its buffer has
         * already been returned to the MediaCodec and released, which means it is already available
         * for the decoding of a following frame, which means that deferring the rendering here
         * (through a sleep or a delayed handler message) is too late, because it does not throttle
         * the decoding loop.
         * This unfortunately means that timed rendering cannot be used in a GL context, and a local
         * {@link Thread#sleep(long)} has to be used in the decoder loop instead.
         */
        notifyFrameAvailability();
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
