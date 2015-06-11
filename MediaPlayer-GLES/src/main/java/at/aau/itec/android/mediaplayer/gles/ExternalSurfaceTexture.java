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

package at.aau.itec.android.mediaplayer.gles;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.view.Surface;

/**
 * @author Mario Guggenberger
 */
public class ExternalSurfaceTexture extends Texture implements SurfaceTexture.OnFrameAvailableListener {

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
        mFrameAvailable = true;
        if(mOnFrameAvailableListener != null) {
            mOnFrameAvailableListener.onFrameAvailable(surfaceTexture);
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
