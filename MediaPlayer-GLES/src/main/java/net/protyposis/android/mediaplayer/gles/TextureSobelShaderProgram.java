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

/**
 * Created by Mario on 07.09.2014.
 */
public class TextureSobelShaderProgram extends TextureShaderProgram {

    private int mThresholdLHandle;
    private int mThresholdHHandle;
    private int mColorHandle;

    public TextureSobelShaderProgram() {
        super("fs_texture_sobel.s");

        mThresholdLHandle = GLES20.glGetUniformLocation(mProgramHandle, "thresholdL");
        GLUtils.checkError("glGetUniformLocation thresholdL");

        mThresholdHHandle = GLES20.glGetUniformLocation(mProgramHandle, "thresholdH");
        GLUtils.checkError("glGetUniformLocation thresholdH");

        mColorHandle = GLES20.glGetUniformLocation(mProgramHandle, "color");
        GLUtils.checkError("glGetUniformLocation color");

        use();
        setThreshold(0.3f, 0.8f);
        setColor(0f, 1f, 0f);
    }

    public void setThreshold(float low, float high) {
        use();
        GLES20.glUniform1f(mThresholdLHandle, low);
        GLES20.glUniform1f(mThresholdHHandle, high);
    }

    public void setColor(float r, float g, float b) {
        use();
        GLES20.glUniform3f(mColorHandle, r, g, b);
    }
}
