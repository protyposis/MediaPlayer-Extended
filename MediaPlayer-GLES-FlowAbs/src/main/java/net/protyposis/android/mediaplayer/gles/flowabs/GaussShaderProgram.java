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

package net.protyposis.android.mediaplayer.gles.flowabs;

import android.opengl.GLES20;

import net.protyposis.android.mediaplayer.gles.GLUtils;

/**
 * Created by maguggen on 17.06.2014.
 */
public class GaussShaderProgram extends FlowabsShaderProgram {

    protected int mSigmaHandle;

    public GaussShaderProgram() {
        super("gauss_fs.glsl");

        mSigmaHandle = GLES20.glGetUniformLocation(mProgramHandle, "sigma");
        GLUtils.checkError("glGetUniformLocation sigma");

        GLES20.glUseProgram(getHandle());
        setSigma(2.0f);
    }

    public void setSigma(float sigma) {
        GLES20.glUniform1f(mSigmaHandle, sigma);
    }
}
