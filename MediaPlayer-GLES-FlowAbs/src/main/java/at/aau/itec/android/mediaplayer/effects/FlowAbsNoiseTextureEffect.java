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
 * Created by Mario on 05.09.2014.
 */
public class FlowAbsNoiseTextureEffect extends FlowAbsSubEffect {
    @Override
    public void apply(Texture2D source, Framebuffer target) {
        mFlowAbsEffect.mFlowAbs.noiseTexture(target);
    }
}
