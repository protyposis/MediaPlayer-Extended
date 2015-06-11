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

package at.aau.itec.android.mediaplayer.gles.qrmarker;

import android.opengl.GLES20;

import at.aau.itec.android.mediaplayer.gles.GLUtils;
import at.aau.itec.android.mediaplayer.gles.TextureShaderProgram;

/**
 * Created by Mario on 07.09.2014.
 */
abstract class QrMarkerShaderProgram extends TextureShaderProgram {

    private static int sSamplerWidth;
    private static int sSamplerHeight;

    public QrMarkerShaderProgram(String fragmentShaderName) {
        super("qrmarker/" + fragmentShaderName);

        mTextureHandle = GLES20.glGetUniformLocation(mProgramHandle, "texture");
        GLUtils.checkError("glGetUniformLocation texture");
    }

    @Override
    protected String preprocessFragmentShaderCode(String fragmentShaderCode) {
        /* Change version to a valid GLES shader language version, add the mandatory
         * precision specifier which needs to come (like everything else) after the version specifier,
         * and add the texture coordinate variable coming from the vertex shader.
         * */
        fragmentShaderCode = fragmentShaderCode.replace("#version 120", "#version 100\n"
                + "precision highp float;\n"
                + "varying vec2 v_TextureCoord;\n");

        /* The texture sampler2D name in the canny_new shader is different from the other shaders.
         * Normalize the name.
         */
        fragmentShaderCode = fragmentShaderCode.replace("uniform sampler2D text;", "uniform sampler2D texture;");
        fragmentShaderCode = fragmentShaderCode.replace("texture2D(text,", "texture2D(texture,");

        /* The sampler texture size is hardcoded as constants.
         * Replace them with the actual dimensions.
         */
        fragmentShaderCode = fragmentShaderCode.replace("1.0 / 640.0;", "1.0 / " + sSamplerWidth + ".0;");
        fragmentShaderCode = fragmentShaderCode.replace("1.0 / 480.0;", "1.0 / " + sSamplerHeight + ".0;");

        /* The gl_TexCoord variable does not exist in GLES. Replace with a valid variable. */
        fragmentShaderCode = fragmentShaderCode.replace("gl_TexCoord[0].st", "v_TextureCoord");

        /* GLES does not support autocasting between float and int, so code needs to be modified
         * in some places to change ints into floats.
         */
        // gauss
        fragmentShaderCode = fragmentShaderCode.replace(
                "vec2(x * texWidth, y * texHeight)",
                "vec2(float(x) * texWidth, float(y) * texHeight)");
        // gradient
        fragmentShaderCode = fragmentShaderCode.replace(
                "2 * texture2D(",
                "2.0 * texture2D(");
        fragmentShaderCode = fragmentShaderCode.replace(
                "vec2(0.0, 1 / 256.0)",
                "vec2(0.0, 1.0 / 256.0)");
        // wideqr
        fragmentShaderCode = fragmentShaderCode.replace(
                "searchDirection * i",
                "searchDirection * float(i)");
        // consense
        fragmentShaderCode = fragmentShaderCode.replace(
                "(windowSize * 2 + 1)",
                "float(windowSize * 2 + 1)");
        fragmentShaderCode = fragmentShaderCode.replace(
                "(1 - abs(mod(hue, 2.0) - 1))",
                "(1.0 - abs(mod(hue, 2.0) - 1.0))");
        fragmentShaderCode = fragmentShaderCode.replace(
                "vec2(i * texWidth, j * texHeight)",
                "vec2(float(i) * texWidth, float(j) * texHeight)");

        /* Enable commented out color consensus result. Very high processing load. */
//        fragmentShaderCode = fragmentShaderCode.replace(
//                "vec4(resp * div, resp * div, resp * div, 1.0);// ",
//                "");

        return fragmentShaderCode;
    }

    /**
     * This method is a VERY UGLY HACK. The QRMarker shaders have their sampler width and height
     * hardcoded as constants, and for the preprocessFragmentShaderCode function to replace them
     * correctly, they need to be known in advance. Passing them in the constructor does not work
     * because the super() call, which in turn calls preprocessFragmentShaderCode, needs to come
     * first, before class variables can be set.
     */
    public static void setTextureSizeHack(int width, int height) {
        sSamplerWidth = width;
        sSamplerHeight = height;
    }
}
