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

package at.aau.itec.android.mediaplayer.gles.flowabs;

import android.opengl.GLES20;

import at.aau.itec.android.mediaplayer.gles.GLUtils;

/**
 * Created by maguggen on 17.06.2014.
 */
public class TextureColorQuantizationShaderProgram extends FlowabsShaderProgram {

    protected int mNumBinsHandle;
    protected int mPhiQHandle;

    public TextureColorQuantizationShaderProgram() {
        super("color_quantization_fs.glsl");

        mNumBinsHandle = GLES20.glGetUniformLocation(mProgramHandle, "nbins");
        GLUtils.checkError("glGetUniformLocation nbins");
        mPhiQHandle = GLES20.glGetUniformLocation(mProgramHandle, "phi_q");
        GLUtils.checkError("glGetUniformLocation phi_q");

        GLES20.glUseProgram(getHandle());
        GLES20.glUniform1i(mNumBinsHandle, 4);
        GLES20.glUniform1f(mPhiQHandle, 3.4f);
    }
}
