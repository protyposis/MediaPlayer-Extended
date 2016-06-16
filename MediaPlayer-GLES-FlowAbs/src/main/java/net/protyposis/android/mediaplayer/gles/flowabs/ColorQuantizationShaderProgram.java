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

package net.protyposis.android.mediaplayer.gles.flowabs;

import android.opengl.GLES20;

import net.protyposis.android.mediaplayer.gles.GLUtils;

/**
 * Created by maguggen on 18.07.2014.
 */
public class ColorQuantizationShaderProgram extends FlowabsShaderProgram {

    protected int mNBinsHandler;
    protected int mPhiQHandler;

    public ColorQuantizationShaderProgram() {
        super("color_quantization_fs.glsl");

        mNBinsHandler = GLES20.glGetUniformLocation(mProgramHandle, "nbins");
        GLUtils.checkError("glGetUniformLocation nbins");
        mPhiQHandler = GLES20.glGetUniformLocation(mProgramHandle, "phi_q");
        GLUtils.checkError("glGetUniformLocation phi_q");

        use();
        setNumBins(4);
        setPhiQ(3.4f);
    }

    public void setNumBins(int numBins) {
        GLES20.glUniform1i(mNBinsHandler, numBins);
    }

    public void setPhiQ(float phiQ) {
        GLES20.glUniform1f(mPhiQHandler, phiQ);
    }
}
