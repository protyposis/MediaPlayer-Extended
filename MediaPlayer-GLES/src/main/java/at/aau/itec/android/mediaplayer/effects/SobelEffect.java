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

package at.aau.itec.android.mediaplayer.effects;

import at.aau.itec.android.mediaplayer.gles.TextureShaderProgram;
import at.aau.itec.android.mediaplayer.gles.TextureSobelShaderProgram;

/**
 * Created by Mario on 07.09.2014.
 */
public class SobelEffect extends ShaderEffect {

    private float mLow, mHigh;
    private float mR, mG, mB;

    public SobelEffect() {
        super("Sobel Edge Detect");
    }

    @Override
    protected TextureShaderProgram initShaderProgram() {
        final TextureSobelShaderProgram sobelShader = new TextureSobelShaderProgram();

        mLow = 0.3f;
        mHigh = 0.8f;
        mR = 0.0f;
        mG = 1.0f;
        mB = 0.0f;

        addParameter(new FloatParameter("Low", 0.0f, 1.0f, mLow, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mLow = value;
                sobelShader.setThreshold(mLow, mHigh);
            }
        }));
        addParameter(new FloatParameter("High", 0.0f, 1.0f, mHigh, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mHigh = value;
                sobelShader.setThreshold(mLow, mHigh);
            }
        }));
        addParameter(new FloatParameter("Red", 0.0f, 1.0f, mR, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mR = value;
                sobelShader.setColor(mR, mG, mB);
            }
        }));
        addParameter(new FloatParameter("Green", 0.0f, 1.0f, mG, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mG = value;
                sobelShader.setColor(mR, mG, mB);
            }
        }));
        addParameter(new FloatParameter("Blue", 0.0f, 1.0f, mB, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mB = value;
                sobelShader.setColor(mR, mG, mB);
            }
        }));

        return sobelShader;
    }
}
