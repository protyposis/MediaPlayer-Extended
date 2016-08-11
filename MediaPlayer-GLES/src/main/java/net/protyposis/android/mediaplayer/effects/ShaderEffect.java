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

import net.protyposis.android.mediaplayer.gles.Framebuffer;
import net.protyposis.android.mediaplayer.gles.GLUtils;
import net.protyposis.android.mediaplayer.gles.Texture2D;
import net.protyposis.android.mediaplayer.gles.TextureShaderProgram;
import net.protyposis.android.mediaplayer.gles.TexturedRectangle;

/**
 * Created by Mario on 18.07.2014.
 */
public abstract class ShaderEffect extends BaseEffect {

    private TexturedRectangle mTexturedRectangle;
    private TextureShaderProgram mShaderProgram;

    protected ShaderEffect(String name) {
        super(name);
    }

    protected ShaderEffect() {
    }

    protected abstract TextureShaderProgram initShaderProgram();

    public void init(int width, int height) {
        getParameters().clear();
        mShaderProgram = initShaderProgram();
        mShaderProgram.setTextureSize(width, height);
        mTexturedRectangle = new TexturedRectangle();
        mTexturedRectangle.reset();
        setInitialized();
    }

    public TextureShaderProgram getShaderProgram() {
        return mShaderProgram;
    }

    @Override
    public void apply(Texture2D source, Framebuffer target) {
        target.bind();
        mShaderProgram.use();
        mShaderProgram.setTexture(source);
        mTexturedRectangle.draw(mShaderProgram);
    }
}
