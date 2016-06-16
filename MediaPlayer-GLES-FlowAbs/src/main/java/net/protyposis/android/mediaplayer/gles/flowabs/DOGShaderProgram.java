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
 * Created by maguggen on 11.07.2014.
 */
public class DOGShaderProgram extends FlowabsShaderProgram {

    protected int mSigmaEHandle;
    protected int mSigmaRHandle;
    protected int mTauHandle;
    protected int mPhiHandle;

    public DOGShaderProgram() {
        super("dog_fs.glsl");

        mSigmaEHandle = GLES20.glGetUniformLocation(mProgramHandle, "sigma_e");
        GLUtils.checkError("glGetUniformLocation sigma_e");
        mSigmaRHandle = GLES20.glGetUniformLocation(mProgramHandle, "sigma_r");
        GLUtils.checkError("glGetUniformLocation sigma_r");
        mTauHandle = GLES20.glGetUniformLocation(mProgramHandle, "tau");
        GLUtils.checkError("glGetUniformLocation tau");
        mPhiHandle = GLES20.glGetUniformLocation(mProgramHandle, "phi");
        GLUtils.checkError("glGetUniformLocation phi");

        use();
        setSigmaE(1.0f);
        setSigmaR(1.6f);
        setTau(0.99f);
        setPhi(2.0f);
    }

    public void setSigmaE(float sigmaE) {
        GLES20.glUniform1f(mSigmaEHandle, sigmaE);
    }
    public void setSigmaR(float sigmaR) {
        GLES20.glUniform1f(mSigmaRHandle, sigmaR);
    }
    public void setTau(float tau) {
        GLES20.glUniform1f(mTauHandle, tau);
    }
    public void setPhi(float phi) {
        GLES20.glUniform1f(mPhiHandle, phi);
    }
}
