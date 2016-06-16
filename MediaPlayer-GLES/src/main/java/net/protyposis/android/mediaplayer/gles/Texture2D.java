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

package net.protyposis.android.mediaplayer.gles;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

import java.nio.Buffer;

/**
 * Created by maguggen on 18.06.2014.
 */
public class Texture2D extends Texture {

    private static final String TAG = Texture2D.class.getSimpleName();

    private int mWidth;
    private int mHeight;

    public Texture2D(int internalformat, int format, int width, int height, int type, Buffer pixels) {
        super();

        mWidth = width;
        mHeight = height;

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        mTexture = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);
        GLUtils.checkError("glBindTexture");

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        // Tegra needs GL_CLAMP_TO_EDGE for non-power-of-2 textures, else the picture is black: http://stackoverflow.com/a/9042198
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, internalformat, mWidth, mHeight, 0, format, type, pixels);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0); // unbind texture
    }

    public Texture2D(int width, int height) {
        this(GLES20.GL_RGBA, GLES20.GL_RGBA, width, height, GLES20.GL_UNSIGNED_BYTE, null);
    }

    /**
     * Sets the filter mode of the texture. Specify -1 to keep the current setting.
     */
    public void setFilterMode(int minFilter, int maxFilter) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);

        if(minFilter > -1) {
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, minFilter);
        }

        if(maxFilter > -1) {
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, maxFilter);
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public static Texture2D generateFloatTexture(int width, int height) {
        if(GLUtils.HAS_GLES30 && GLUtils.HAS_GL_OES_texture_half_float && GLUtils.HAS_FLOAT_FRAMEBUFFER_SUPPORT) {
            return new Texture2D(GLES30.GL_RGBA16F, GLES20.GL_RGBA, width, height, GLES20.GL_FLOAT, null);
        } else {
            Log.i(TAG, "Texture fallback mode to GLES20 8 bit");
            return new Texture2D(GLES30.GL_RGBA, GLES20.GL_RGBA, width, height, GLES20.GL_UNSIGNED_BYTE, null);
        }
    }
}
