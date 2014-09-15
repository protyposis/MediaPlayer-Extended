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
import android.opengl.GLES30;
import android.util.Log;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import at.aau.itec.android.mediaplayer.gles.GLUtils;
import at.aau.itec.android.mediaplayer.gles.Texture2D;

/**
 * Created by maguggen on 11.07.2014.
 */
public class RandomLuminanceNoiseTexture extends Texture2D {

    private static final String TAG = RandomLuminanceNoiseTexture.class.getSimpleName();

    private RandomLuminanceNoiseTexture(int internalformat, int format, int width, int height, int type, Buffer pixels) {
        super(internalformat, format, width, height, type, pixels);
    }

    public static RandomLuminanceNoiseTexture generate(int width, int height) {
        float[] noise = new float[width * height];
        Random rnd = new Random(1);

        // Noise generation code ported from FlowAbs GLView::open function; could be simplified/Java-ified
        // What the hell does it exactly do?
        int w = width;
        int h = height;
        int p = 0;
        for (int j = 0; j < h; ++j) {
            for (int i = 0; i < w; ++i) {
                noise[p++] = 0.5f + 2.0f * (rnd.nextFloat() - 0.5f);
            }
        }
        p = 0;
        for(int j = 0; j < h; ++j) {
            noise[p] = (3*noise[p] + noise[p+1]) / 4;
            ++p;
            for(int i = 1; i < w - 1; ++i) {
                noise[p] = (noise[p-1] + 2*noise[p] + noise[p+1]) / 4;
                ++p;
            }
            noise[p] = (noise[p-1] + 3*noise[p]) / 4;
            ++p;
        }
        p = 0;
        for (int i = 0; i < w; ++i) {
            noise[p] = (3*noise[p] + noise[p+w]) / 4;
            p++;
        }
        for (int j = 1; j < h-1; ++j) {
            for (int i = 0; i < w; ++i) {
                noise[p] = (noise[p-w] + 2*noise[p] + noise[p+w]) / 4;
                ++p;
            }
        }
        for (int i = 0; i < w; ++i) {
            noise[p] = (noise[p-width] + 3*noise[p]) / 4;
            ++p;
        }

        if(GLUtils.HAS_GLES30 && GLUtils.HAS_GL_OES_texture_half_float) {
            /* GLES supports LUMINANCE texture upload only for 8 bit byte textures. For
             * (Half)float data needs to be blown up to RGB. */
            float[] rgbNoise = new float[noise.length * 3];
            for (int i = 0; i < noise.length; i++) {
                rgbNoise[i * 3 + 0] = noise[i];
                rgbNoise[i * 3 + 1] = noise[i];
                rgbNoise[i * 3 + 2] = noise[i];
            }

            FloatBuffer pixels = ByteBuffer.allocateDirect(rgbNoise.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(rgbNoise);
            pixels.rewind();

            return new RandomLuminanceNoiseTexture(GLES30.GL_RGB16F, GLES20.GL_RGB, width, height, GLES20.GL_FLOAT, pixels);
        } else {
            Log.i(TAG, "Texture fallback mode to GLES20 8 bit");

            byte[] byteNoise = new byte[width * height];
            for(int i = 0; i < noise.length; i++) {
                byteNoise[i] = (byte)(noise[i] * 255);
            }
            ByteBuffer pixels = ByteBuffer.allocateDirect(byteNoise.length).put(byteNoise);
            pixels.rewind();

            return new RandomLuminanceNoiseTexture(GLES20.GL_RGBA, GLES20.GL_LUMINANCE, width, height, GLES20.GL_UNSIGNED_BYTE, pixels);
        }
    }
}
