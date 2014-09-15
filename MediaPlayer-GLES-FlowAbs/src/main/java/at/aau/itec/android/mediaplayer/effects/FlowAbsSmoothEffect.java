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

import at.aau.itec.android.mediaplayer.gles.Framebuffer;
import at.aau.itec.android.mediaplayer.gles.Texture2D;

/**
 * Created by Mario on 18.07.2014.
 */
public class FlowAbsSmoothEffect extends FlowAbsSubEffect {

    private int mType;
    private float mSigma;

    FlowAbsSmoothEffect() {
        super();
        mType = 1;
        mSigma = 1.0f;

        addParameter(new IntegerParameter("Type", 0, 3, mType, new IntegerParameter.Delegate() {
            @Override
            public void setValue(int value) {
                mType = value;
            }
        }));
        addParameter(new FloatParameter("Sigma", 0f, 10f, mSigma, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mSigma = value;
            }
        }));
    }

    @Override
    public void apply(Texture2D source, Framebuffer target) {
        mFlowAbsEffect.mFlowAbs.smoothFilter(source, target, mType, mSigma);
    }
}
