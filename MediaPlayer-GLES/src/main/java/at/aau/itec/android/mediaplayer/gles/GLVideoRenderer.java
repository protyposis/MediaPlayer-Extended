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

package at.aau.itec.android.mediaplayer.gles;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import at.aau.itec.android.mediaplayer.effects.Effect;

/**
 * Created by Mario on 14.06.2014.
 */
public class GLVideoRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = GLVideoRenderer.class.getSimpleName();

    public enum RenderRequest {
        DEFAULT,
        ALL,
        EFFECT,
        GEOMETRY
    }

    /**
     * Callback interface for being notified when the ExternalSurfaceTexture is created, which
     * can be used to feed picture frames from the MediaCodec or camera preview. Note that
     * the callback comes from the GLThread so you need to take care in the handler of
     * dispatching the action to the right thread, e.g. the UI thread.
     */
    public interface OnExternalSurfaceTextureCreatedListener {
        void onExternalSurfaceTextureCreated(ExternalSurfaceTexture surfaceTexture);
    }

    public interface OnEffectInitializedListener {
        void onEffectInitialized(Effect effect);
    }

    public interface OnFrameCapturedCallback {
        void onFrameCaptured(Bitmap bitmap);
    }

    /**
     * The view matrix / camera position
     */
    private float[] mViewMatrix = new float[16];

    /**
     * The projection matrix / camera frame
     */
    private float[] mProjectionMatrix = new float[16];

    private int mWidth;
    private int mHeight;

    private ExternalSurfaceTexture mExternalSurfaceTexture;
    private ReadExternalTextureShaderProgram mReadExternalTextureShaderProgram;
    private Framebuffer mFramebufferIn;
    private Framebuffer mFramebufferOut;
    private TexturedRectangle mTexturedRectangle;
    private TextureShaderProgram mTextureToScreenShaderProgram;

    private TextureShaderProgram mDefaultShaderProgram;
    private List<Effect> mEffects;
    private Effect mEffect;
    private RenderRequest mRenderRequest;

    private OnExternalSurfaceTextureCreatedListener mOnExternalSurfaceTextureCreatedListener;
    private OnEffectInitializedListener mOnEffectInitializedListener;
    private FrameRateCalculator mFrameRateCalculator;

    public GLVideoRenderer() {
        Log.d(TAG, "ctor");

        mTexturedRectangle = new TexturedRectangle();

        mEffects = new ArrayList<Effect>();
    }

    public void setOnExternalSurfaceTextureCreatedListener(OnExternalSurfaceTextureCreatedListener l) {
        this.mOnExternalSurfaceTextureCreatedListener = l;
    }

    public void setOnEffectInitializedListener(OnEffectInitializedListener l) {
        this.mOnEffectInitializedListener = l;
    }

    public void setRenderRequest(RenderRequest renderRequest) {
        mRenderRequest = renderRequest;
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated");
        GLUtils.init();
        GLUtils.printSysConfig();

        // set the background color
        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);

        // set up the "camera"
        Matrix.setLookAtM(mViewMatrix, 0,
                0.0f, 0.0f, 1.0f,   // eye x,y,z
                0.0f, 0.0f, 0.0f,  // look x,y,z
                0.0f, 1.0f, 0.0f);  // up x,y,z

        mExternalSurfaceTexture = new ExternalSurfaceTexture();
        mReadExternalTextureShaderProgram = new ReadExternalTextureShaderProgram();

        mDefaultShaderProgram = new TextureShaderProgram();

        mTextureToScreenShaderProgram = new TextureShaderProgram();

        if(mOnExternalSurfaceTextureCreatedListener != null) {
            mOnExternalSurfaceTextureCreatedListener.onExternalSurfaceTextureCreated(mExternalSurfaceTexture);
        }

        mFrameRateCalculator = new FrameRateCalculator(30);
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        Log.d(TAG, "onSurfaceChanged " + width + "x" + height);

        mFramebufferIn = new Framebuffer(width, height);
        mFramebufferOut = new Framebuffer(width, height);
        mFramebufferOut.getTexture().setFilterMode(-1, GLES20.GL_LINEAR);

        for(Effect effect : mEffects) {
            /* After a surface change, if the resolution has changed, effects need to be
             * reinitialized to update them to the new resolution. */
            if(effect.isInitialized()) {
                Log.d(TAG, "reinitializing effect " + effect.getName());
                effect.init(width, height);
            }
        }

        // adjust the viewport to the surface size
        GLES20.glViewport(0, 0, width, height);
        mWidth = width;
        mHeight = height;

        setZoomLevel(1.0f);

        // fully re-render current scene to adjust to the change
        mRenderRequest = RenderRequest.ALL;
        onDrawFrame(glUnused);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {

        // PREPARE

        mTexturedRectangle.reset();


        // FETCH AND TRANSFER FRAME TO TEXTURE
        if(mRenderRequest == RenderRequest.ALL || mExternalSurfaceTexture.isTextureUpdateAvailable()) {
            mExternalSurfaceTexture.updateTexture();

            mFramebufferIn.bind();
            mReadExternalTextureShaderProgram.use();
            mReadExternalTextureShaderProgram.setTexture(mExternalSurfaceTexture);
            mTexturedRectangle.draw(mReadExternalTextureShaderProgram);

            mRenderRequest = RenderRequest.EFFECT;
        }


        // MANIPULATE TEXTURE WITH SHADER(S)

        if(mRenderRequest == RenderRequest.EFFECT) {
            if (mEffect != null) {
                mEffect.apply(mFramebufferIn.getTexture(), mFramebufferOut);
            } else {
                mFramebufferOut.bind();
                mDefaultShaderProgram.use();
                mDefaultShaderProgram.setTexture(mFramebufferIn.getTexture());
                mTexturedRectangle.draw(mDefaultShaderProgram);
            }

            mRenderRequest = RenderRequest.GEOMETRY;
        }


        // RENDER TEXTURE TO SCREEN

        if(mRenderRequest == RenderRequest.GEOMETRY) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0); // framebuffer 0 is the screen
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
            mTextureToScreenShaderProgram.use();
            mTextureToScreenShaderProgram.setTexture(mFramebufferOut.getTexture());

            mTexturedRectangle.translate(0.0f, 0.0f, -1.0f);
            mTexturedRectangle.calculateMVP(mViewMatrix, mProjectionMatrix);

            mTexturedRectangle.draw(mTextureToScreenShaderProgram);
        }

        // STUFF

        //mFrameRateCalculator.frame();
        mRenderRequest = RenderRequest.DEFAULT;
    }

    public void setZoomLevel(float zoomLevel) {
        Matrix.orthoM(mProjectionMatrix, 0,
                -1.0f / zoomLevel, 1.0f / zoomLevel,
                -1.0f / zoomLevel, 1.0f / zoomLevel,
                1.0f, 10.0f);
    }

    public void setPan(float pX, float pY) {
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.translateM(mViewMatrix, 0, pX, pY, 0.0f);
    }

    public void addEffect(Effect... effects) {
        for(Effect effect : effects) {
            Log.d(TAG, "adding effect " + effect.getName());
            mEffects.add(effect);
        }
    }

    public void selectEffect(int index) {
        if(index >= mEffects.size()) {
            Log.w(TAG, String.format("invalid effect index %d (%d effects registered)",
                    index, mEffects.size()));
            return;
        }
        mEffect = mEffects.get(index);
        if(!mEffect.isInitialized()) {
            Log.d(TAG, "initializing effect " + mEffect.getName());
            mEffect.init(mWidth, mHeight);
            if(mOnEffectInitializedListener != null) {
                mOnEffectInitializedListener.onEffectInitialized(mEffect);
            }
        }
    }

    public void saveCurrentFrame(OnFrameCapturedCallback callback) {
        callback.onFrameCaptured(GLUtils.getFrameBuffer(mWidth, mHeight));
    }
}
