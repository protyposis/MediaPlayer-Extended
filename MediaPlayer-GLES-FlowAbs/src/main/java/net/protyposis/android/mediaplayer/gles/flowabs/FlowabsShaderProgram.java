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
import net.protyposis.android.mediaplayer.gles.TextureShaderProgram;

/**
 * Created by maguggen on 17.06.2014.
 */
public class FlowabsShaderProgram extends TextureShaderProgram {

    public FlowabsShaderProgram(String fragmentShaderName) {
        super("flowabs/" + fragmentShaderName);

        mTextureHandle = GLES20.glGetUniformLocation(mProgramHandle, "img");
        GLUtils.checkError("glGetUniformLocation img");

        mTextureSizeHandle = GLES20.glGetUniformLocation(mProgramHandle, "img_size");
        GLUtils.checkError("glGetUniformLocation img_size");
    }

    @Override
    protected String preprocessFragmentShaderCode(String fragmentShaderCode) {
        /* Add precision specifier which is mandatory in GLES but missing in the flowabs shaders
         * because they are normal OpenGL 2.0 shaders.
         * Add v_TextureCoord declaration */
        fragmentShaderCode = "precision highp float;\n" +
                "\n" +
                "varying vec2 v_TextureCoord;\n" +
                fragmentShaderCode;

        /* Replace uv coordinate calculation with v_TextureCoord because usage of gl_FragCoord results
         * in an error 0x501 on some platforms. */
        fragmentShaderCode = fragmentShaderCode.replace(
                "vec2 uv = gl_FragCoord.xy / img_size;",
                "vec2 uv = v_TextureCoord;");

        return fragmentShaderCode;
    }
}
