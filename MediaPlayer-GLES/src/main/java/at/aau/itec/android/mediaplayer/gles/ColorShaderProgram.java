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
public class ColorShaderProgram extends ShaderProgram {

    protected int mMVPMatrixHandle;
    protected int mPositionHandle;
    protected int mColorHandle;

    public ColorShaderProgram() {
        super("vs_color.s", "fs_color.s");

        // NOTE this could be moved to shape objects (would result in more overhead by multiple calls,
        //      but gives more freedom with the usage of shader variables)
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        GLUtils.checkError("glGetUniformLocation u_MVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        GLUtils.checkError("glGetAttribLocation a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
        GLUtils.checkError("glGetAttribLocation a_Color");
    }
}
