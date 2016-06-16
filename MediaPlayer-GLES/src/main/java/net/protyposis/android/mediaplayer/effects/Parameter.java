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
 * Created by maguggen on 21.08.2014.
 */
public abstract class Parameter {

    interface Listener {
        void onParameterChanged(Parameter parameter);
    }

    public enum Type {
        INTEGER,
        FLOAT
    }

    private String mName;
    private Type mType;
    private Listener mListener;

    protected Parameter(String name, Type type) {
        mName = name;
        mType = type;
    }

    public Type getType() {
        return mType;
    }

    public String getName() {
        return mName;
    }

    public abstract void reset();

    public void setListener(Listener listener) {
        mListener = listener;
    }

    protected void fireParameterChanged() {
        if(mListener != null) {
            mListener.onParameterChanged(this);
        }
    }
}
