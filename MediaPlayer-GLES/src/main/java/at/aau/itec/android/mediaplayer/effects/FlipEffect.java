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

package at.aau.itec.android.mediaplayer.effects;

import at.aau.itec.android.mediaplayer.gles.TextureFlipShaderProgram;
import at.aau.itec.android.mediaplayer.gles.TextureShaderProgram;

/**
 * Created by maguggen on 22.08.2014.
 */
public class FlipEffect extends ShaderEffect {

    private int mMode;

    @Override
    protected TextureShaderProgram initShaderProgram() {
        final TextureFlipShaderProgram flipShader = new TextureFlipShaderProgram();
        mMode = 1;

        flipShader.setMode(mMode);

        addParameter(new IntegerParameter("Mode", 0, 3, mMode, new IntegerParameter.Delegate() {
            @Override
            public void setValue(int value) {
                mMode = value;
                flipShader.setMode(mMode);
            }
        }));

        return flipShader;
    }
}
