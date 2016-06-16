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

package net.protyposis.android.mediaplayer.gles.flowabs;

import net.protyposis.android.mediaplayer.gles.Framebuffer;
import net.protyposis.android.mediaplayer.gles.Texture2D;
import net.protyposis.android.mediaplayer.gles.TextureShaderProgram;
import net.protyposis.android.mediaplayer.gles.TexturedRectangle;

/**
 * Created by maguggen on 11.07.2014.
 */
public class FlowAbs {

    private Framebuffer mFramebuffer1;
    private Framebuffer mFramebuffer2;
    private Framebuffer mFramebuffer3;
    private Framebuffer mFramebuffer4;
    private Framebuffer mFramebuffer5;
    private Framebuffer mFramebuffer6;
    private Framebuffer mFramebuffer7;
    private Framebuffer mFramebuffer8;

    private RandomLuminanceNoiseTexture mNoiseTexture;

    private TexturedRectangle mTexturedRectangle;

    private SmoothedStructureTensorShaderProgram mSstShader;
    private GaussShaderProgram mGaussShader;
    private TextureGauss3x3ShaderProgram mGauss3x3Shader;
    private TextureGauss5x5ShaderProgram mGauss5x5Shader;
    private TangentFlowMapShaderProgram mTfmShader;
    private LineIntegralConvolutionShaderProgram mLicShader;
    private DOGShaderProgram mDogShader;
    private RGB2LABShaderProgram mRgb2LabShader;
    private LAB2RGBShaderProgram mLab2RgbShader;
    private FDOG0ShaderProgram mFdog0Shader;
    private FDOG1ShaderProgram mFdog1Shader;
    private TextureShaderProgram mTextureCopyShader;
    private OrientationAlignedBilateralFilterShaderProgram mBilateralFilterShader;
    private ColorQuantizationShaderProgram mColorQuantizationShader;
    private MixWithEdgesShaderProgram mMixEdgesShader;
    private OverlayShaderProgram mOverlayShader;

    public FlowAbs(int width, int height) {
        mTexturedRectangle = new TexturedRectangle();
        mTexturedRectangle.reset();

        mSstShader = new SmoothedStructureTensorShaderProgram();
        mSstShader.setTextureSize(width, height);

        mGaussShader = new GaussShaderProgram();
        mGaussShader.setTextureSize(width, height);

        mGauss3x3Shader = new TextureGauss3x3ShaderProgram();
        mGauss3x3Shader.setTextureSize(width, height);

        mGauss5x5Shader = new TextureGauss5x5ShaderProgram();
        mGauss5x5Shader.setTextureSize(width, height);

        mTfmShader = new TangentFlowMapShaderProgram();
        mTfmShader.setTextureSize(width, height);

        mLicShader = new LineIntegralConvolutionShaderProgram();
        mLicShader.setTextureSize(width, height);

        mDogShader = new DOGShaderProgram();
        mDogShader.setTextureSize(width, height);

        mRgb2LabShader = new RGB2LABShaderProgram();
        mRgb2LabShader.setTextureSize(width, height);

        mLab2RgbShader = new LAB2RGBShaderProgram();
        mLab2RgbShader.setTextureSize(width, height);

        mFdog0Shader = new FDOG0ShaderProgram();
        mFdog0Shader.setTextureSize(width, height);

        mFdog1Shader = new FDOG1ShaderProgram();
        mFdog1Shader.setTextureSize(width, height);

        mTextureCopyShader = new TextureShaderProgram();
        mTextureCopyShader.setTextureSize(width, height);

        mBilateralFilterShader = new OrientationAlignedBilateralFilterShaderProgram();
        mBilateralFilterShader.setTextureSize(width, height);

        mColorQuantizationShader = new ColorQuantizationShaderProgram();
        mColorQuantizationShader.setTextureSize(width, height);

        mMixEdgesShader = new MixWithEdgesShaderProgram();
        mMixEdgesShader.setTextureSize(width, height);

        mOverlayShader = new OverlayShaderProgram();
        mOverlayShader.setTextureSize(width, height);

        mFramebuffer1 = new Framebuffer(width, height);
        mFramebuffer2 = new Framebuffer(width, height);
        mFramebuffer3 = new Framebuffer(width, height);
        mFramebuffer4 = new Framebuffer(width, height);
        mFramebuffer5 = new Framebuffer(width, height);
        mFramebuffer6 = new Framebuffer(width, height);
        mFramebuffer7 = new Framebuffer(width, height);
        mFramebuffer8 = new Framebuffer(width, height);

        mNoiseTexture = RandomLuminanceNoiseTexture.generate(width, height);
    }

    private void copy(Texture2D source, Framebuffer target) {
        target.bind();
        mTextureCopyShader.use();
        mTextureCopyShader.setTexture(source);
        mTexturedRectangle.draw(mTextureCopyShader);
    }

    private void tangentFlowMap(Texture2D source, Framebuffer target, Framebuffer tmp1, float sigma) {
        target.bind();
        mSstShader.use();
        mSstShader.setTexture(source);
        mTexturedRectangle.draw(mSstShader);

        tmp1.bind();
        mGaussShader.use();
        mGaussShader.setTexture(target.getTexture());
        mGaussShader.setSigma(sigma);
        mTexturedRectangle.draw(mGaussShader);

        target.bind();
        mTfmShader.use();
        mTfmShader.setTexture(tmp1.getTexture());
        mTexturedRectangle.draw(mTfmShader);
    }

    public void tangentFlowMap(Texture2D source, Framebuffer target, float sigma) {
        tangentFlowMap(source, mFramebuffer1, mFramebuffer2, sigma);

        //copy(mFramebuffer1.getTexture(), target);

        target.bind();
        mLicShader.use();
        mLicShader.setTexture(mNoiseTexture, mFramebuffer1.getTexture());
        mLicShader.setSigma(5.0f);
        mTexturedRectangle.draw(mLicShader);
    }

    public void gauss(Texture2D source, Framebuffer target, float sigma) {
        target.bind();
        mGaussShader.use();
        mGaussShader.setSigma(sigma);
        mGaussShader.setTexture(source);
        mTexturedRectangle.draw(mGaussShader);
    }

    private void smoothFilter(Texture2D source, Texture2D tfm, Framebuffer target, int type, float sigma) {
        target.bind();
        if(type == 3) {
            mLicShader.use();
            mLicShader.setTexture(source, tfm);
            mLicShader.setSigma(sigma);
            mTexturedRectangle.draw(mLicShader);
        } else {
            TextureShaderProgram gauss = (type == 1 ? mGauss3x3Shader : mGauss5x5Shader);
            gauss.use();
            gauss.setTexture(source);
            mTexturedRectangle.draw(gauss);
        }
    }

    public void smoothFilter(Texture2D source, Framebuffer target, int type, float sigma) {
        if(type == 0) {
            copy(source, target);
        } else {
            if(type == 3) {
                tangentFlowMap(source, mFramebuffer1, mFramebuffer2, sigma);
            }
            smoothFilter(source, mFramebuffer1.getTexture(), target, type, sigma);
        }
    }

    /**
     * DEBUG method to check if noise texture is ok
     */
    public void noiseTexture(Framebuffer target) {
        copy(mNoiseTexture, target);
    }

    private void bilateralFilter(Texture2D lab, Texture2D tfm, Framebuffer target, int n, float sigmaD, float sigmaR, Framebuffer tmp1) {
        mBilateralFilterShader.use();
        mBilateralFilterShader.setSigmaD(sigmaD);
        mBilateralFilterShader.setSigmaR(sigmaR);

        for(int i = 0; i < n; ++i) {
            tmp1.bind();
            mBilateralFilterShader.setTexture(i == 0 ? lab : target.getTexture(), tfm); // TODO set GL_LINEAR ?
            mBilateralFilterShader.setPass(0);
            mTexturedRectangle.draw(mBilateralFilterShader);

            target.bind();
            mBilateralFilterShader.setTexture(tmp1.getTexture(), tfm); // TODO set GL_LINEAR ?
            mBilateralFilterShader.setPass(1);
            mTexturedRectangle.draw(mBilateralFilterShader);
        }
    }

    public void bilateralFilter(Texture2D source, Framebuffer target, float gaussSigma, int n, float sigmaD, float sigmaR) {
        rgb2lab(source, mFramebuffer1);
        if(n > 0) {
            tangentFlowMap(source, mFramebuffer2, mFramebuffer3, gaussSigma);
            bilateralFilter(mFramebuffer1.getTexture(), mFramebuffer2.getTexture(), mFramebuffer3, n, sigmaD, sigmaR, mFramebuffer4);
            lab2rgb(mFramebuffer3.getTexture(), target);
        } else {
            lab2rgb(mFramebuffer1.getTexture(), target);
        }
    }

    private void dog(Texture2D source, Framebuffer target, Framebuffer tmp1, int n, float sigmaE, float sigmaR, float tau, float phi) {
        for (int i = 0; i < n; ++i) {
            Texture2D src = source;
            if (i > 0) {
                overlay(target.getTexture(), source, tmp1);
                src = tmp1.getTexture();
            }
            target.bind();
            mDogShader.use();
            mDogShader.setTexture(src);
            mDogShader.setSigmaE(sigmaE);
            mDogShader.setSigmaR(sigmaR);
            mDogShader.setTau(tau);
            mDogShader.setPhi(phi);
            mTexturedRectangle.draw(mDogShader);
        }
    }

    public void dog(Texture2D source, Framebuffer target, int n, float sigmaE, float sigmaR, float tau, float phi) {
        dog(source, target, mFramebuffer1, n, sigmaE, sigmaR, tau, phi);
    }

    public void rgb2lab(Texture2D source, Framebuffer target) {
        target.bind();
        mRgb2LabShader.use();
        mRgb2LabShader.setTexture(source);
        mTexturedRectangle.draw(mRgb2LabShader);
    }

    public void lab2rgb(Texture2D source, Framebuffer target) {
        target.bind();
        mLab2RgbShader.use();
        mLab2RgbShader.setTexture(source);
        mTexturedRectangle.draw(mLab2RgbShader);
    }

    private void fdog(Texture2D labOrBfeSource, Texture2D tfm, Framebuffer target,
                      Framebuffer tmp1, Framebuffer tmp2, Framebuffer tmp3,
                      int n, float sigmaE, float sigmaR, float tau, float sigmaM, float phi) {
        for (int i = 0; i < n; ++i) {
            Texture2D src = labOrBfeSource;
            if(i > 0) {
                overlay(tmp3.getTexture(), labOrBfeSource, tmp2);
                src = tmp2.getTexture();
            }
            tmp1.bind();
            mFdog0Shader.use();
            mFdog0Shader.setTexture(src, tfm);
            mFdog0Shader.setSigmaE(sigmaE);
            mFdog0Shader.setSigmaR(sigmaR);
            mFdog0Shader.setTau(tau);
            mTexturedRectangle.draw(mFdog0Shader);

            (i == (n-1) ? target : tmp3).bind();
            mFdog1Shader.use();
            mFdog1Shader.setTexture(tmp1.getTexture(), tfm);
            mFdog1Shader.setSigmaM(sigmaM);
            mFdog1Shader.setPhi(phi);
            mTexturedRectangle.draw(mFdog1Shader);
        }
    }

    public void fdog(Texture2D source, Framebuffer target, float gaussSigma,
                     int n, float sigmaE, float sigmaR, float tau, float sigmaM, float phi) {
        rgb2lab(source, mFramebuffer1);
        tangentFlowMap(source, mFramebuffer2, mFramebuffer3, gaussSigma);
        fdog(mFramebuffer1.getTexture(), mFramebuffer2.getTexture(), target,
                mFramebuffer3, mFramebuffer4, mFramebuffer5, n, sigmaE, sigmaR, tau, sigmaM, phi);
    }

    private void colorQuantization(Texture2D source, Framebuffer target, Framebuffer tmp1, int filter, int numBins, float phiQ) {
        (filter > 0 ? tmp1 : target).bind();
        mColorQuantizationShader.use();
        mColorQuantizationShader.setNumBins(numBins);
        mColorQuantizationShader.setPhiQ(phiQ);
        mColorQuantizationShader.setTexture(source);
        mTexturedRectangle.draw(mColorQuantizationShader);

        if(filter > 0) {
            FlowabsShaderProgram gaussShader = (filter == 1) ? mGauss3x3Shader : mGauss5x5Shader;
            target.bind();
            gaussShader.use();
            gaussShader.setTexture(tmp1.getTexture());
            mTexturedRectangle.draw(gaussShader);
        }
    }

    public void colorQuantization(Texture2D source, Framebuffer target, int filter, int numBins, float phiQ) {
        rgb2lab(source, mFramebuffer1); // TODO should be bilateral filter
        colorQuantization(mFramebuffer1.getTexture(), mFramebuffer2, mFramebuffer3, filter, numBins, phiQ);
        lab2rgb(mFramebuffer2.getTexture(), target);
    }

    public void mix(Texture2D source, Texture2D edges, Framebuffer target, float[] edgeColor) {
        target.bind();
        mMixEdgesShader.use();
        mMixEdgesShader.setColor(edgeColor[0], edgeColor[1], edgeColor[2]);
        mMixEdgesShader.setTexture(source, edges);
        mTexturedRectangle.draw(mMixEdgesShader);
    }

    public void overlay(Texture2D source, Texture2D edges, Framebuffer target) {
        target.bind();
        mOverlayShader.use();
        mOverlayShader.setTexture(source, edges);
        mTexturedRectangle.draw(mOverlayShader);
    }

    public void flowAbs(Texture2D source, Framebuffer target,
                        float sstSigma,
                        int bfNE, int bfNA, float bfSigmaD, float bfSigmaR,
                        int fdogType, int fdogN, float fdogSigmaE, float fdogSigmaR, float fdogSigmaM, float fdogTau, float fdogPhi,
                        int cqFilter, int cqNumBins, float cqPhiQ,
                        float[] edgeColor,
                        int fsType, float fsSigma) {
        rgb2lab(source, mFramebuffer1); // -> FB1 lab
        tangentFlowMap(source, mFramebuffer2, mFramebuffer3, sstSigma); // -> FB2 tfm
        if(bfNE > 0) {
            bilateralFilter(mFramebuffer1.getTexture(), mFramebuffer2.getTexture(), mFramebuffer3, bfNE, bfSigmaD, bfSigmaR, mFramebuffer4); // -> FB3 bfe
        }
        if(bfNA > 0) {
            bilateralFilter(mFramebuffer1.getTexture(), mFramebuffer2.getTexture(), mFramebuffer4, bfNE, bfSigmaD, bfSigmaR, mFramebuffer5); // -> FB4 bfa
        }
        if(fdogType == 0) {
            fdog((bfNE > 0 ? mFramebuffer3 : mFramebuffer1).getTexture(), mFramebuffer2.getTexture(),
                    mFramebuffer5, mFramebuffer6, mFramebuffer7, mFramebuffer8,
                    fdogN, fdogSigmaE, fdogSigmaR, fdogTau, fdogSigmaM, fdogPhi); // -> FB5 fdog edges
        } else {
            dog((bfNE > 0 ? mFramebuffer3 : mFramebuffer1).getTexture(), mFramebuffer5, mFramebuffer6, fdogN, fdogSigmaE, fdogSigmaR, fdogTau, fdogPhi); // -> FB5 dog edges
        }
        // FB3 bfe free
        colorQuantization((bfNA > 0 ? mFramebuffer4 : mFramebuffer1).getTexture(), mFramebuffer3, mFramebuffer6, cqFilter, cqNumBins, cqPhiQ); // -> FB3 cq
        // FB1 lab free
        // FB4 bfa free
        lab2rgb(mFramebuffer3.getTexture(), mFramebuffer1); // -> FB1 cq_rgb
        // FB3 cq free
        mix(mFramebuffer1.getTexture(), mFramebuffer5.getTexture(), mFramebuffer3, edgeColor); // -> FS3 ov
        // FB1 cq_rgb free
        // FB5 edges free
        if(fsType == 0) {
            copy(mFramebuffer3.getTexture(), target);
        } else {
            smoothFilter(mFramebuffer3.getTexture(), mFramebuffer2.getTexture(), target, fsType, fsSigma);
        }
        // FB* free
    }
}
