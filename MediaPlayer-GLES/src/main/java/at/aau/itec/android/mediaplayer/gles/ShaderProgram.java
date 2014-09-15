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
import android.util.Log;

import java.security.InvalidParameterException;

import at.aau.itec.android.mediaplayer.LibraryHelper;

/**
 * Created by Mario on 14.06.2014.
 */
public class ShaderProgram {

    private static final String TAG = ShaderProgram.class.getSimpleName();

    private int mVShaderHandle;
    private int mFShaderHandle;
    protected int mProgramHandle;

    public ShaderProgram(String vertexShaderName, String fragmentShaderName) {
        String vertexShaderCode = LibraryHelper.loadTextFromAsset("shaders/" + vertexShaderName);
        String fragmentShaderCode = LibraryHelper.loadTextFromAsset("shaders/" + fragmentShaderName);

        vertexShaderCode = preprocessVertexShaderCode(vertexShaderCode);
        fragmentShaderCode = preprocessFragmentShaderCode(fragmentShaderCode);

        mVShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        mFShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgramHandle = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgramHandle, mVShaderHandle);
        GLUtils.checkError("glAttachShader V");
        GLES20.glAttachShader(mProgramHandle, mFShaderHandle);
        GLUtils.checkError("glAttachShader F");
        GLES20.glLinkProgram(mProgramHandle);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(mProgramHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Error linking program: " + GLES20.glGetProgramInfoLog(mProgramHandle));
            GLES20.glDeleteProgram(mProgramHandle);
        }

        // delete the shaders after compiling the program to free some space (if they will not be reused later)
        // http://stackoverflow.com/questions/9113154/proper-way-to-delete-glsl-shader
        deleteShader(mVShaderHandle);
        deleteShader(mFShaderHandle);
    }

    public void deleteProgram() {
        GLES20.glDeleteProgram(mProgramHandle);
        GLUtils.checkError("glDeleteProgram");
    }

    public int getHandle() {
        return mProgramHandle;
    }

    public static int loadShader(int type, String shaderCode) {
        if(type != GLES20.GL_VERTEX_SHADER && type != GLES20.GL_FRAGMENT_SHADER) {
            throw new InvalidParameterException("invalid shader type");
        }

        int handle = GLES20.glCreateShader(type);
        if(handle != 0) {
            GLES20.glShaderSource(handle, shaderCode);
            GLES20.glCompileShader(handle);

            int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(handle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            if (compileStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(handle));
                deleteShader(handle);
                handle = 0;
            }
        } else {
            GLUtils.checkError("glCreateShader");
        }

        return handle;
    }

    public static void deleteShader(int shaderHandle) {
        GLES20.glDeleteShader(shaderHandle);
        GLUtils.checkError("glDeleteShader");
    }

    public void use() {
        GLES20.glUseProgram(mProgramHandle);
    }

    protected String preprocessVertexShaderCode(String vertexShaderCode) {
        return vertexShaderCode;
    }

    protected String preprocessFragmentShaderCode(String fragmentShaderCode) {
        return fragmentShaderCode;
    }
}
