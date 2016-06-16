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
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by Mario on 14.06.2014.
 */
public class ColoredRectangle extends Shape {

    /**
     * Elements per vertex
     * */
    protected static final int sStrideBytes = 7 * sBytesPerFloat;

    /**
     * Offset of the position data
     * */
    protected static final int sPositionOffset = 0;

    /**
     * Size of the position data in elements
     * */
    protected static final int sPositionDataSize = 3;

    /**
     * Offset of the color data
     * */
    protected static final int sColorOffset = 3;

    /**
     * Size of the color data in elements
     * */
    protected static final int sColorDataSize = 4;

    // model data
    private float[] mVerticesData = {
            // X, Y, Z,
            // R, G, B, A
            -1.0f, -1.0f, 0.0f,
            1.0f, 0.0f, 0.0f, 1.0f,

            1.0f, -1.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 1.0f,

            -1.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 1.0f,

            1.0f, 1.0f, 0.0f,
            1.0f, 1.0f, 1.0f, 1.0f
    };

    // model data buffer
    private FloatBuffer mVertices;

    private ColorShaderProgram mShaderProgram;

    public ColoredRectangle() {
        mVertices = ByteBuffer.allocateDirect(mVerticesData.length * sBytesPerFloat)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mVertices.put(mVerticesData).position(0);
    }

    public void setShaderProgram(ColorShaderProgram shaderProgram) {
        mShaderProgram = shaderProgram;
    }

    public void draw(float[] mvpMatrix) {
        if(mShaderProgram == null) {
            Log.d("ColorRectangle", "no shader set");
            return;
        }

        // write vertex data
        mVertices.position(sPositionOffset);
        GLES20.glVertexAttribPointer(mShaderProgram.mPositionHandle, sPositionDataSize,
                GLES20.GL_FLOAT, false, sStrideBytes, mVertices);
        GLES20.glEnableVertexAttribArray(mShaderProgram.mPositionHandle);

        // write color data
        mVertices.position(sColorOffset);
        GLES20.glVertexAttribPointer(mShaderProgram.mColorHandle, sColorDataSize,
                GLES20.GL_FLOAT, false, sStrideBytes, mVertices);
        GLES20.glEnableVertexAttribArray(mShaderProgram.mColorHandle);

        // write the MVP matrix
        GLES20.glUniformMatrix4fv(mShaderProgram.mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // finally, render the rectangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }
}
