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

import android.opengl.GLES20;

/**
 * Created by Mario on 14.06.2014.
 */
public class TextureShaderProgram extends ShaderProgram {

    protected int mMVPMatrixHandle;
    protected int mPositionHandle;
    protected int mTextureCoordHandle;
    protected int mSTMatrixHandle;
    protected int mTextureSizeHandle;
    protected int mTextureHandle;

    public TextureShaderProgram() {
        this("vs_texture.s", "fs_texture.s");
    }

    public TextureShaderProgram(String fragmentShaderName) {
        this("vs_texture.s", fragmentShaderName);
    }

    protected TextureShaderProgram(String vertexShaderName, String fragmentShaderName) {
        super(vertexShaderName, fragmentShaderName);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        GLUtils.checkError("glGetUniformLocation u_MVPMatrix");
        mSTMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_STMatrix");
        GLUtils.checkError("glGetAttribLocation u_STMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        GLUtils.checkError("glGetAttribLocation a_Position");
        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TextureCoord");
        GLUtils.checkError("glGetAttribLocation a_TextureCoord");
        mTextureSizeHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_TextureSize");
        GLUtils.checkError("glGetUniformLocation u_TextureSize");
        mTextureHandle = GLES20.glGetUniformLocation(mProgramHandle, "s_Texture");
        GLUtils.checkError("glGetUniformLocation s_Texture");
    }

    public void setTextureSize(int width, int height) {
        use();
        GLES20.glUniform2f(mTextureSizeHandle, width, height);
    }

    public void setTexture(Texture2D texture) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.getHandle());
        GLES20.glUniform1i(mTextureHandle, 0); // bind texture unit 0 to the uniform
        GLES20.glUniformMatrix4fv(mSTMatrixHandle, 1, false, texture.getTransformMatrix(), 0);
    }
}
