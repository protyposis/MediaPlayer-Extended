/*
 * Copyright (c) 2014 Mario Guggenberger <mg@protyposis.net>
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

import android.opengl.Matrix;

/**
 * Created by maguggen on 04.07.2014.
 */
public abstract class Texture {

    protected int mTexture;

    protected float[] mTransformMatrix = new float[16];

    public Texture() {
        Matrix.setIdentityM(mTransformMatrix, 0);
    }

    public int getHandle() {
        return mTexture;
    }

    public float[] getTransformMatrix() {
        return mTransformMatrix;
    }
}
