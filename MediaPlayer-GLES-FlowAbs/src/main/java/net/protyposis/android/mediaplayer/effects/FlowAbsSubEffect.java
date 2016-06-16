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

package net.protyposis.android.mediaplayer.effects;

/**
 * Created by Mario on 18.07.2014.
 *
 * Used to share a common FlowAbs instance across all FlowAbs subeffects. Initializing one FlowAbs
 * instance per effect takes too much time and memory.
 */
public abstract class FlowAbsSubEffect extends BaseEffect {

    protected FlowAbsEffect mFlowAbsEffect;

    FlowAbsSubEffect(String name) {
        super(name);
    }

    FlowAbsSubEffect() {
    }

    @Override
    public void init(int width, int height) {
        if(!mFlowAbsEffect.isInitialized()) {
            mFlowAbsEffect.init(width, height);
        }
    }

    FlowAbsSubEffect init(FlowAbsEffect flowAbsEffect) {
        mFlowAbsEffect = flowAbsEffect;
        return this;
    }
}
