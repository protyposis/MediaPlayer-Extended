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

package at.aau.itec.android.mediaplayer.effects;

import at.aau.itec.android.mediaplayer.gles.Framebuffer;
import at.aau.itec.android.mediaplayer.gles.Texture2D;
import at.aau.itec.android.mediaplayer.gles.flowabs.FlowAbs;

/**
 * Created by Mario on 18.07.2014.
 */
public class FlowAbsEffect extends BaseEffect {

    protected FlowAbs mFlowAbs;

    private float mSstSigma;

    private int mBfNE;
    private int mBfNA;
    private float mBfSigmaD;
    private float mBfSigmaR;

    private int mFDogType;
    private int mFDogN;
    private float mFDogSigmaE;
    private float mFDogSigmaR;
    private float mFDogSigmaM;
    private float mFDogTau;
    private float mFDogPhi;

    private int mCqFilter;
    private int mCqNumBins;
    private float mCqPhiQ;

    private float[] mEdgeColor;

    private int mFsType;
    private float mFsSigma;

    public FlowAbsEffect() {
        super();

        mSstSigma = 2.0f;

        mBfNE = 0; // TODO default to 1 once the bilateral filter is working correctly
        mBfNA = 0; // TODO default to 4 once the bilateral filter is working correctly
        mBfSigmaD = 3.0f;
        mBfSigmaR = 4.25f;

        mFDogType = 0;
        mFDogN = 1;
        mFDogSigmaE = 1.0f;
        mFDogSigmaR = 1.6f;
        mFDogSigmaM = 3.0f;
        mFDogTau = 0.99f;
        mFDogPhi = 2.0f;

        mCqFilter = 1;
        mCqNumBins = 8;
        mCqPhiQ = 3.4f;

        mEdgeColor = new float[] { 0.0f, 0.0f, 0.0f };

        mFsType = 1;
        mFsSigma = 1.0f;

        addParameter(new FloatParameter("SST Sigma", 0f, 10f, mSstSigma, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mSstSigma = value;
            }
        }));

        addParameter(new IntegerParameter("BF N E", 0, 10, mBfNE, new IntegerParameter.Delegate() {
            @Override
            public void setValue(int value) {
                mBfNE = value;
            }
        }));
        addParameter(new IntegerParameter("BF N A", 0, 10, mBfNA, new IntegerParameter.Delegate() {
            @Override
            public void setValue(int value) {
                mBfNA = value;
            }
        }));
        addParameter(new FloatParameter("BF sigmaD", 0f, 10f, mBfSigmaD, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mBfSigmaD = value;
            }
        }));
        addParameter(new FloatParameter("BF sigmaR", 0f, 10f, mBfSigmaR, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mBfSigmaR = value;
            }
        }));

        addParameter(new IntegerParameter("(F)DOG Type", 0, 1, mFDogType, new IntegerParameter.Delegate() {
            @Override
            public void setValue(int value) {
                mFDogType = value;
            }
        }));
        addParameter(new IntegerParameter("(F)DOG N", 0, 10, mFDogN, new IntegerParameter.Delegate() {
            @Override
            public void setValue(int value) {
                mFDogN = value;
            }
        }));
        addParameter(new FloatParameter("(F)DOG sigmaE", 0f, 10f, mFDogSigmaE, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mFDogSigmaE = value;
            }
        }));
        addParameter(new FloatParameter("(F)DOG sigmaR", 0f, 10f, mFDogSigmaR, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mFDogSigmaR = value;
            }
        }));
        addParameter(new FloatParameter("FDOG sigmaM", 0f, 10f, mFDogSigmaM, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mFDogSigmaM = value;
            }
        }));
        addParameter(new FloatParameter("(F)DOG tau", 0f, 10f, mFDogTau, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mFDogTau = value;
            }
        }));
        addParameter(new FloatParameter("(F)DOG phi", 0f, 10f, mFDogPhi, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mFDogPhi = value;
            }
        }));

        addParameter(new IntegerParameter("CQ Filter", 0, 2, mCqFilter, new IntegerParameter.Delegate() {
            @Override
            public void setValue(int value) {
                mCqFilter = value;
            }
        }));
        addParameter(new IntegerParameter("CQ Bins", 0, 20, mCqNumBins, new IntegerParameter.Delegate() {
            @Override
            public void setValue(int value) {
                mCqNumBins = value;
            }
        }));
        addParameter(new FloatParameter("CQ phiQ", 0f, 10f, mCqPhiQ, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mCqPhiQ = value;
            }
        }));

        addParameter(new FloatParameter("Edge R", 0f, 1f, mEdgeColor[0], new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mEdgeColor[0] = value;
            }
        }));
        addParameter(new FloatParameter("Edge G", 0f, 1f, mEdgeColor[1], new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mEdgeColor[1] = value;
            }
        }));
        addParameter(new FloatParameter("Edge B", 0f, 1f, mEdgeColor[2], new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mEdgeColor[2] = value;
            }
        }));

        addParameter(new IntegerParameter("FS Type", 0, 3, mFsType, new IntegerParameter.Delegate() {
            @Override
            public void setValue(int value) {
                mFsType = value;
            }
        }));
        addParameter(new FloatParameter("FS Sigma", 0f, 10f, mFsSigma, new FloatParameter.Delegate() {
            @Override
            public void setValue(float value) {
                mFsSigma = value;
            }
        }));
    }

    @Override
    public void init(int width, int height) {
        mFlowAbs = new FlowAbs(width, height);
        setInitialized();
    }

    @Override
    public void apply(Texture2D source, Framebuffer target) {
        mFlowAbs.flowAbs(source, target,
                mSstSigma,
                mBfNE, mBfNA, mBfSigmaD, mBfSigmaR,
                mFDogType, mFDogN, mFDogSigmaE, mFDogSigmaR, mFDogSigmaM, mFDogTau, mFDogPhi,
                mCqFilter, mCqNumBins, mCqPhiQ,
                mEdgeColor,
                mFsType, mFsSigma);
    }

    public FlowAbsGaussEffect getGaussEffect() {
        return (FlowAbsGaussEffect) new FlowAbsGaussEffect().init(this);
    }

    public FlowAbsSmoothEffect getSmoothEffect() {
        return (FlowAbsSmoothEffect) new FlowAbsSmoothEffect().init(this);
    }

    public FlowAbsBilateralFilterEffect getBilateralFilterEffect() {
        return (FlowAbsBilateralFilterEffect) new FlowAbsBilateralFilterEffect().init(this);
    }

    public FlowAbsColorQuantizationEffect getColorQuantizationEffect() {
        return (FlowAbsColorQuantizationEffect) new FlowAbsColorQuantizationEffect().init(this);
    }

    public FlowAbsDOGEffect getDOGEffect() {
        return (FlowAbsDOGEffect) new FlowAbsDOGEffect().init(this);
    }

    public FlowAbsFDOGEffect getFDOGEffect() {
        return (FlowAbsFDOGEffect) new FlowAbsFDOGEffect().init(this);
    }

    public FlowAbsTangentFlowMapEffect getTangentFlowMapEffect() {
        return (FlowAbsTangentFlowMapEffect) new FlowAbsTangentFlowMapEffect().init(this);
    }

    public FlowAbsNoiseTextureEffect getNoiseTextureEffect() {
        return (FlowAbsNoiseTextureEffect) new FlowAbsNoiseTextureEffect().init(this);
    }
}
