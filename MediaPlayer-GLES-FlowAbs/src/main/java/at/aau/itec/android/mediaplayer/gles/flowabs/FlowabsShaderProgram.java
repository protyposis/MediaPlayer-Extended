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
import at.aau.itec.android.mediaplayer.gles.TextureShaderProgram;

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
        /* add precision specifier which is mandatory in GLES but missing in the flowabs shaders
         * because they are normal OpenGL 2.0 shaders.
         */
        fragmentShaderCode = "precision highp float;\n" + fragmentShaderCode;

        /* Replace uv coordinate calculation fitted for OpenGL GL_CLAMP with one fitted
         * for GLES GL_CLAMP_TO_EDGE for a pixel perfect mapping.
         */
        fragmentShaderCode = fragmentShaderCode.replace(
                "vec2 uv = gl_FragCoord.xy / img_size;",
                "vec2 uv = vec2(0.5, 0.5) / img_size + gl_FragCoord.xy / img_size;");

        return fragmentShaderCode;
    }
}
