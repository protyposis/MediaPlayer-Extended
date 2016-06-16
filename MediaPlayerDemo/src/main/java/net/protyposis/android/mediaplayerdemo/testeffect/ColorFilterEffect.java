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

package net.protyposis.android.mediaplayerdemo.testeffect;

import net.protyposis.android.mediaplayer.effects.FloatParameter;
import net.protyposis.android.mediaplayer.effects.ShaderEffect;
import net.protyposis.android.mediaplayer.gles.TextureShaderProgram;

/**
 * Created by Mario on 19.07.2014.
 */
public class ColorFilterEffect extends ShaderEffect {

    private float mR, mG, mB, mA;

    @Override
    protected TextureShaderProgram initShaderProgram() {
        final ColorFilterShaderProgram colorFilterShader = new ColorFilterShaderProgram();

        mR = 1.0f;
        mG = 0.0f;
        mB = 0.0f;
        mA = 1.0f;

        colorFilterShader.setColor(mR, mG, mB, mA);

        addParameter(new FloatParameter("Red", 0.0f, 1.0f, mR, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mR = value;
                colorFilterShader.setColor(mR, mG, mB, mA);
            }
        }));
        addParameter(new FloatParameter("Green", 0.0f, 1.0f, mG, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mG = value;
                colorFilterShader.setColor(mR, mG, mB, mA);
            }
        }));
        addParameter(new FloatParameter("Blue", 0.0f, 1.0f, mB, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mB = value;
                colorFilterShader.setColor(mR, mG, mB, mA);
            }
        }));
        addParameter(new FloatParameter("Alpha", 0.0f, 1.0f, mA, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mA = value;
                colorFilterShader.setColor(mR, mG, mB, mA);
            }
        }));

        return colorFilterShader;
    }
}
