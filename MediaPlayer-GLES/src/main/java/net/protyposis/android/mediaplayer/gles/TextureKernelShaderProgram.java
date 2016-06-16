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
 * Created by maguggen on 16.06.2014.
 */
public class TextureKernelShaderProgram extends TextureShaderProgram {

    public enum Kernel {

        BLUR(new float[] {
                1f/9f, 1f/9f, 1f/9f,
                1f/9f, 1f/9f, 1f/9f,
                1f/9f, 1f/9f, 1f/9f }),

        BLUR_GAUSS(new float[] {
                1f/16f, 2f/16f, 1f/16f,
                2f/16f, 4f/16f, 2f/16f,
                1f/16f, 2f/16f, 1f/16f }),

        SHARPEN(new float[] {
                0f, -1f, 0f,
                -1f, 5f, -1f,
                0f, -1f, 0f }),

        EDGE_DETECT(new float[] {
                0f, 1f, 0f,
                1f, -4f, 1f,
                0f, 1f, 0f }),

        EMBOSS(new float[] {
                -2f, -1f, 0f,
                -1f, 1f, 1f,
                0f, 1f, 2f });

        float[] mKernel;

        Kernel(float[] kernel) {
            mKernel = kernel;
        }
    }

    protected int mKernelHandle;
    protected int mTexOffsetHandle;
    protected int mColorAdjustHandle;

    public TextureKernelShaderProgram(Kernel kernel) {
        super("fs_texture_kernel.s");

        mKernelHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Kernel");
        GLUtils.checkError("glGetUniformLocation u_Kernel");
        mTexOffsetHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_TexOffset");
        GLUtils.checkError("glGetUniformLocation u_TexOffset");

        setKernel(kernel);
    }

    public void setKernel(Kernel kernel) {
        GLES20.glUseProgram(getHandle());
        GLES20.glUniform1fv(mKernelHandle, 9, kernel.mKernel, 0);
    }

    @Override
    public void setTextureSize(int width, int height) {
        //super.setTextureSize(width, height);
        float rw = 1.0f / width;
        float rh = 1.0f / height;

        float texOffset[] = new float[] {
                -rw, -rh,   0f, -rh,    rw, -rh,
                -rw, 0f,    0f, 0f,     rw, 0f,
                -rw, rh,    0f, rh,     rw, rh
        };

        GLES20.glUseProgram(getHandle());
        GLES20.glUniform2fv(mTexOffsetHandle, 9, texOffset, 0);
    }
}
