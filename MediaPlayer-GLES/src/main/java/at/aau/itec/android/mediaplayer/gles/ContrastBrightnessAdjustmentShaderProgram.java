/*
 * Copyright (c) 2014 Mario Guggenberger <mario.guggenberger@aau.at>
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

import android.opengl.GLES20;

/**
 * Created by Mario on 02.10.2014.
 */
public class ContrastBrightnessAdjustmentShaderProgram extends TextureShaderProgram {

    private int mContrastHandle;
    private int mBrightnessHandle;

    public ContrastBrightnessAdjustmentShaderProgram() {
        super("fs_adjust_contrast_brightness.s");

        mContrastHandle = GLES20.glGetUniformLocation(mProgramHandle, "contrast");
        GLUtils.checkError("glGetUniformLocation contrast");

        mBrightnessHandle = GLES20.glGetUniformLocation(mProgramHandle, "brightness");
        GLUtils.checkError("glGetUniformLocation brightness");

        setContrast(1.0f);
        setBrightness(1.0f);
    }

    public void setContrast(float contrast) {
        use();
        GLES20.glUniform1f(mContrastHandle, contrast);
    }

    public void setBrightness(float brightness) {
        use();
        GLES20.glUniform1f(mBrightnessHandle, brightness);
    }
}
