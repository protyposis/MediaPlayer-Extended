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

package net.protyposis.android.mediaplayer.effects;

import java.util.ArrayList;
import java.util.List;

import net.protyposis.android.mediaplayer.gles.Framebuffer;
import net.protyposis.android.mediaplayer.gles.Texture2D;

/**
 * Created by Mario on 18.07.2014.
 */
abstract class BaseEffect implements Effect, Parameter.Listener {

    private String mName;
    private List<Parameter> mParameters;
    private boolean mInitialized;
    private Listener mListener;

    public BaseEffect(String name) {
        if(name == null) {
            name = this.getClass().getSimpleName();
            // remove "effect" suffix when applicable
            if(name.endsWith("Effect")) {
                name = name.substring(0, name.length() - 6);
            }
        }
        mName = name;
        mParameters = new ArrayList<Parameter>();
    }

    public BaseEffect() {
        this(null);
    }

    public String getName() {
        return mName;
    }

    public abstract void init(int width, int height);

    @Override
    public boolean isInitialized() {
        return mInitialized;
    }

    public abstract void apply(Texture2D source, Framebuffer target);

    @Override
    public void addParameter(Parameter parameter) {
        mParameters.add(parameter);
        parameter.setListener(this);
    }

    @Override
    public List<Parameter> getParameters() {
        return mParameters;
    }

    @Override
    public boolean hasParameters() {
        return mParameters != null && !mParameters.isEmpty();
    }

    protected void setInitialized() {
        mInitialized = true;
    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public void onParameterChanged(Parameter parameter) {
        if(mListener != null) {
            mListener.onEffectChanged(this);
        }
    }
}
