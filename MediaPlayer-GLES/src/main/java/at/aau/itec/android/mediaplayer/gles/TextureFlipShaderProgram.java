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
public class TextureFlipShaderProgram extends TextureShaderProgram {

    protected int mModeHandle;

    public TextureFlipShaderProgram() {
        super("fs_texture_flip.s");

        mModeHandle = GLES20.glGetUniformLocation(mProgramHandle, "mode");
        GLUtils.checkError("glGetUniformLocation mode");

        use();
        setMode(0);
    }

    public void setMode(int mode) {
        if(mode < 0 || mode > 3) {
            throw new RuntimeException("mode must be in range [0, 3]");
        }
        use();
        GLES20.glUniform1i(mModeHandle, mode);
    }
}
