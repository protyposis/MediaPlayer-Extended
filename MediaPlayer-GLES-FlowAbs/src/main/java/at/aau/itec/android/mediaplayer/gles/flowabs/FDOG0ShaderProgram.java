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

package at.aau.itec.android.mediaplayer.gles.flowabs;

import android.opengl.GLES20;

import at.aau.itec.android.mediaplayer.gles.GLUtils;
import at.aau.itec.android.mediaplayer.gles.Texture2D;

/**
 * Created by maguggen on 11.07.2014.
 */
public class FDOG0ShaderProgram extends FlowabsShaderProgram {

    protected int mTextureHandle2;
    protected int mSigmaEHandle;
    protected int mSigmaRHandle;
    protected int mTauHandle;

    public FDOG0ShaderProgram() {
        super("fdog0_fs.glsl");

        mTextureHandle = GLES20.glGetUniformLocation(mProgramHandle, "img");
        GLUtils.checkError("glGetUniformLocation img");

        mTextureHandle2 = GLES20.glGetUniformLocation(mProgramHandle, "tfm");
        GLUtils.checkError("glGetUniformLocation tfm");

        mSigmaEHandle = GLES20.glGetUniformLocation(mProgramHandle, "sigma_e");
        GLUtils.checkError("glGetUniformLocation sigma_e");
        mSigmaRHandle = GLES20.glGetUniformLocation(mProgramHandle, "sigma_r");
        GLUtils.checkError("glGetUniformLocation sigma_r");
        mTauHandle = GLES20.glGetUniformLocation(mProgramHandle, "tau");
        GLUtils.checkError("glGetUniformLocation tau");

        use();
        setSigmaE(1.0f);
        setSigmaR(1.6f);
        setTau(0.99f);
    }

    @Override
    public void setTexture(Texture2D texture) {
        throw new RuntimeException("not supported!!!");
    }

    public void setTexture(Texture2D img, Texture2D tfm) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, img.getHandle());
        GLES20.glUniform1i(mTextureHandle, 0); // bind texture unit 0 to the uniform

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tfm.getHandle());
        GLES20.glUniform1i(mTextureHandle2, 1); // bind texture unit 0 to the uniform

        GLES20.glUniformMatrix4fv(mSTMatrixHandle, 1, false, img.getTransformMatrix(), 0);
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
}
