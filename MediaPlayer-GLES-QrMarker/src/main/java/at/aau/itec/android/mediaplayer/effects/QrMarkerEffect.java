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

import at.aau.itec.android.mediaplayer.gles.Framebuffer;
import at.aau.itec.android.mediaplayer.gles.Texture2D;
import at.aau.itec.android.mediaplayer.gles.TexturedRectangle;
import at.aau.itec.android.mediaplayer.gles.qrmarker.CannyShaderProgram;
import at.aau.itec.android.mediaplayer.gles.qrmarker.ConsenseShaderProgram;
import at.aau.itec.android.mediaplayer.gles.qrmarker.GaussShaderProgram;
import at.aau.itec.android.mediaplayer.gles.qrmarker.GradientShaderProgram;
import at.aau.itec.android.mediaplayer.gles.qrmarker.QrResponseShaderProgram;

/**
 * Created by Mario on 07.09.2014.
 */
public class QrMarkerEffect extends BaseEffect {

    private GaussShaderProgram mGaussShader;
    private GradientShaderProgram mGradientShader;
    private CannyShaderProgram mCannyShader;
    private QrResponseShaderProgram mQrResponseShader;
    private ConsenseShaderProgram mConsensusShader;

    private Framebuffer mFramebuffer1;
    private Framebuffer mFramebuffer2;

    private TexturedRectangle mTexturedRectangle;

    private CannyEdgeEffect mCannyEdgeEffect;

    public QrMarkerEffect() {
        mCannyEdgeEffect = new CannyEdgeEffect();
    }

    @Override
    public void init(int width, int height) {
        // PART OF THE UGLY HACK described in setTextureSizeHack
        // Cannot call it on base class QrMarkerShaderProgram because it is hidden outside its package
        GaussShaderProgram.setTextureSizeHack(width, height);

        mGaussShader = new GaussShaderProgram();
        mGaussShader.setTextureSize(width, height);

        mGradientShader = new GradientShaderProgram();
        mGradientShader.setTextureSize(width, height);

        mCannyShader = new CannyShaderProgram();
        mCannyShader.setTextureSize(width, height);

        mQrResponseShader = new QrResponseShaderProgram();
        mQrResponseShader.setTextureSize(width, height);

        mConsensusShader = new ConsenseShaderProgram();
        mConsensusShader.setTextureSize(width, height);

        mFramebuffer1 = new Framebuffer(width, height);
        mFramebuffer2 = new Framebuffer(width, height);

        mTexturedRectangle = new TexturedRectangle();
        mTexturedRectangle.reset();

        setInitialized();
    }

    @Override
    public void apply(Texture2D source, Framebuffer target) {
        applyCannyEdge(source, mFramebuffer1);

        mFramebuffer2.bind();
        mQrResponseShader.use();
        mQrResponseShader.setTexture(mFramebuffer1.getTexture());
        mTexturedRectangle.draw(mQrResponseShader);

        target.bind();
        mConsensusShader.use();
        mConsensusShader.setTexture(mFramebuffer2.getTexture());
        mTexturedRectangle.draw(mConsensusShader);
    }

    private void applyCannyEdge(Texture2D source, Framebuffer target) {
        mFramebuffer1.bind();
        mGaussShader.use();
        mGaussShader.setTexture(source);
        mTexturedRectangle.draw(mGaussShader);

        mFramebuffer2.bind();
        mGradientShader.use();
        mGradientShader.setTexture(mFramebuffer1.getTexture());
        mTexturedRectangle.draw(mGradientShader);

        target.bind();
        mCannyShader.use();
        mCannyShader.setTexture(mFramebuffer2.getTexture());
        mTexturedRectangle.draw(mCannyShader);
    }

    public CannyEdgeEffect getCannyEdgeEffect() {
        return mCannyEdgeEffect;
    }

    /**
     * The CannyEdge Effect is a subeffect of the QrMarker Effect, it is therefore more efficient
     * to share the resources and reuse a common cannyedge subroutine than to instantiate it as
     * a separate effect. If one of the two effects is needed, the other comes with it for free.
     */
    public class CannyEdgeEffect extends BaseEffect {

        @Override
        public void init(int width, int height) {
            if(!QrMarkerEffect.this.isInitialized()) {
                QrMarkerEffect.this.init(width, height);
            }
        }

        @Override
        public void apply(Texture2D source, Framebuffer target) {
            applyCannyEdge(source, target);
        }
    }
}
