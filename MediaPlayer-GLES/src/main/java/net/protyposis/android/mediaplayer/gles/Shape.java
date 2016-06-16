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

import android.opengl.Matrix;

/**
 * Created by Mario on 14.06.2014.
 */
public abstract class Shape {

    /**
     * bytes per float
     */
    protected static final int sBytesPerFloat = 4;

    /**
     * The model matrix positions models in world space
     */
    public float[] mModelMatrix = new float[16];

    /**
     * The final model-view-projection matrix used for rendering
     * NOTE: could be made static to save space, since objects calculate their matrix sequentially
     */
    protected float[] mMVPMatrix = new float[16];

    public void reset() {
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.setIdentityM(mMVPMatrix, 0);
    }

    public void translate(float x, float y, float z) {
        Matrix.translateM(mModelMatrix, 0, x, y, z);
    }

    public void calculateMVP(float[] viewMatrix, float[] projectionMatrix) {
        Matrix.multiplyMM(mMVPMatrix, 0, viewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, projectionMatrix, 0, mMVPMatrix, 0);
    }
}
