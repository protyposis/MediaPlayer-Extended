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

package at.aau.itec.android.mediaplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.*;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import at.aau.itec.android.mediaplayer.effects.Effect;
import at.aau.itec.android.mediaplayer.gles.*;

/**
 * Created by Mario on 14.06.2014.
 */
public class GLTextureView extends GLSurfaceView implements
        GLVideoRenderer.OnExternalSurfaceTextureCreatedListener,
        SurfaceTexture.OnFrameAvailableListener,
        Effect.Listener, GLVideoRenderer.OnEffectInitializedListener,
        GLVideoRenderer.OnFrameCapturedCallback {

    private static final String TAG = GLTextureView.class.getSimpleName();

    public interface OnEffectInitializedListener extends GLVideoRenderer.OnEffectInitializedListener {}
    public interface OnFrameCapturedCallback extends GLVideoRenderer.OnFrameCapturedCallback {}

    private GLVideoRenderer mRenderer;
    private Handler mRunOnUiThreadHandler = new Handler();
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;

    private OnEffectInitializedListener mOnEffectInitializedListener;
    private OnFrameCapturedCallback mOnFrameCapturedCallback;

    private float mZoomLevel = 1.0f;
    private float mZoomSnappingRange = 0.02f;
    private float mPanX;
    private float mPanY;
    private float mPanSnappingRange = 0.02f;

    protected int mVideoWidth;
    protected int mVideoHeight;

    protected GLTextureView(Context context) {
        super(context);
        init(context);
    }

    protected GLTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        if(isInEditMode()) {
            // do not start renderer in layout editor
            return;
        }
        if(!at.aau.itec.android.mediaplayer.gles.GLUtils.isGlEs2Supported(context)) {
            Log.e(TAG, "GLES 2.0 is not supported");
            return;
        }

        LibraryHelper.setContext(context);

        mRenderer = new GLVideoRenderer();
        mRenderer.setOnExternalSurfaceTextureCreatedListener(this);
        mRenderer.setOnEffectInitializedListener(this);

        setEGLContextClientVersion(2);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mScaleGestureDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        mZoomLevel *= detector.getScaleFactor();

                        if(LibraryHelper.isBetween(mZoomLevel, 1-mZoomSnappingRange, 1+mZoomSnappingRange)) {
                            mZoomLevel = 1.0f;
                        }

                        // limit zooming to magnification zooms (zoom-ins)
                        if(mZoomLevel < 1.0f) {
                            mZoomLevel = 1.0f;
                        }

                        setZoom(mZoomLevel);
                        return true;
                    }
                });

        mGestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                        // divide by zoom level to adjust panning speed to zoomed picture size
                        // multiply by fixed scaling factor to compensate for panning lag
                        mPanX += distanceX / getWidth() / mZoomLevel * 1.2f;
                        mPanY += distanceY / getHeight() / mZoomLevel * 1.2f;

                        float panSnappingRange = mPanSnappingRange / mZoomLevel;
                        if(LibraryHelper.isBetween(mPanX, -panSnappingRange, +panSnappingRange)) {
                            mPanX = 0;
                        }
                        if(LibraryHelper.isBetween(mPanY, -panSnappingRange, +panSnappingRange)) {
                            mPanY = 0;
                        }

                        // limit panning to the texture bounds so it always covers the complete view
                        float maxPanX = Math.abs((1.0f / mZoomLevel) - 1.0f);
                        float maxPanY = Math.abs((1.0f / mZoomLevel) - 1.0f);
                        mPanX = LibraryHelper.clamp(mPanX, -maxPanX, maxPanX);
                        mPanY = LibraryHelper.clamp(mPanY, -maxPanY, maxPanY);

                        setPan(mPanX, mPanY);
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        mZoomLevel = 1;
                        mPanX = 0;
                        mPanY = 0;

                        setZoom(mZoomLevel);
                        setPan(mPanX, mPanY);

                        return true;
                    }
                });
    }

    /**
     * Sets the zoom factor of the texture in the view. 1.0 means no zoom, 2.0 2x zoom, etc.
     */
    public void setZoom(float zoomFactor) {
        mZoomLevel = zoomFactor;
        mRenderer.setZoomLevel(mZoomLevel);
        requestRender(GLVideoRenderer.RenderRequest.GEOMETRY);
    }

    public float getZoomLevel() {
        return mZoomLevel;
    }

    /**
     * Sets the panning of the texture in the view. (0.0, 0.0) centers the texture and means no
     * panning, (-1.0, -1.0) moves the texture to the lower right quarter.
     * @param x
     * @param y
     */
    public void setPan(float x, float y) {
        mPanX = x;
        mPanY = y;
        mRenderer.setPan(-mPanX, mPanY);
        requestRender(GLVideoRenderer.RenderRequest.GEOMETRY);
    }

    public float getPanX() {
        return mPanX;
    }

    public float getPanY() {
        return mPanY;
    }

    /**
     * Resizes the video view according to the video size to keep aspect ratio.
     * Code copied from {@link android.widget.VideoView#onMeasure(int, int)}.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Log.i("@@@@", "onMeasure(" + MeasureSpec.toString(widthMeasureSpec) + ", "
        //        + MeasureSpec.toString(heightMeasureSpec) + ")");

        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        if (mVideoWidth > 0 && mVideoHeight > 0) {

            int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
                // the size is fixed
                width = widthSpecSize;
                height = heightSpecSize;

                // for compatibility, we adjust size based on aspect ratio
                if ( mVideoWidth * height  < width * mVideoHeight ) {
                    //Log.i("@@@", "image too wide, correcting");
                    width = height * mVideoWidth / mVideoHeight;
                } else if ( mVideoWidth * height  > width * mVideoHeight ) {
                    //Log.i("@@@", "image too tall, correcting");
                    height = width * mVideoHeight / mVideoWidth;
                }
            } else if (widthSpecMode == MeasureSpec.EXACTLY) {
                // only the width is fixed, adjust the height to match aspect ratio if possible
                width = widthSpecSize;
                height = width * mVideoHeight / mVideoWidth;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    height = heightSpecSize;
                }
            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
                // only the height is fixed, adjust the width to match aspect ratio if possible
                height = heightSpecSize;
                width = height * mVideoWidth / mVideoHeight;
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    width = widthSpecSize;
                }
            } else {
                // neither the width nor the height are fixed, try to use actual video size
                width = mVideoWidth;
                height = mVideoHeight;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // too tall, decrease both width and height
                    height = heightSpecSize;
                    width = height * mVideoWidth / mVideoHeight;
                }
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // too wide, decrease both width and height
                    width = widthSpecSize;
                    height = width * mVideoHeight / mVideoWidth;
                }
            }
        } else {
            // no size yet, just adopt the given spec sizes
        }
        setMeasuredDimension(width, height);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        /* A view unfortunately cannot be transparent to touch events (pass them to a lower layer)
         * and at the same time consume them, which would be desirable here for the view to detect
         * pan and zoom gestures but still hand touches down so the activity can toggle the
         * MediaController when touching this view.
         * The workaround is to never consume touch events by returning false here and always
         * handing them to the next layer. The target layer (e.g. activity) then processes the event
         * as it wants, and should pass it on to this class' #onTouchEvent.
         */
        return false;
    }

    @Override
    public void onExternalSurfaceTextureCreated(final ExternalSurfaceTexture surfaceTexture) {
        // dispatch event to UI thread
        mRunOnUiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                onSurfaceTextureCreated(surfaceTexture.getSurfaceTexture());
            }
        });

        surfaceTexture.setOnFrameAvailableListener(GLTextureView.this);
    }

    /**
     * Event handler that gets called when the video surface is ready to be used.
     * Can be overwritten in a subclass.
     * @param surfaceTexture the video surface texture
     */
    public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture) {
        // nothing to do here
    }

    public void addEffect(final Effect... effects) {
        for(Effect effect : effects) {
            effect.setListener(this);
        }
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.addEffect(effects);
            }
        });
    }

    public void selectEffect(final int index) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.selectEffect(index);
                requestRender(GLVideoRenderer.RenderRequest.EFFECT);
            }
        });
    }

    public void setOnEffectInitializedListener(OnEffectInitializedListener listener) {
        mOnEffectInitializedListener = listener;
    }

    public void setOnFrameCapturedCallback(OnFrameCapturedCallback callback) {
        mOnFrameCapturedCallback = callback;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender(GLVideoRenderer.RenderRequest.ALL);
    }

    @Override
    public void onEffectInitialized(Effect effect) {
        if(mOnEffectInitializedListener != null) {
            mOnEffectInitializedListener.onEffectInitialized(effect);
        }
        requestRender(GLVideoRenderer.RenderRequest.EFFECT);
    }

    @Override
    public void onEffectChanged(Effect effect) {
        requestRender(GLVideoRenderer.RenderRequest.EFFECT);
    }

    protected void requestRender(final GLVideoRenderer.RenderRequest renderRequest) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setRenderRequest(renderRequest);
                requestRender();
            }
        });
    }

    /**
     * Async request of the current frame from the GL renderer. The result will become available
     * on the UI thread in #onFrameCaptured(Bitmap).
     */
    public void captureFrame() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.saveCurrentFrame(new GLVideoRenderer.OnFrameCapturedCallback() {
                    @Override
                    public void onFrameCaptured(final Bitmap bitmap) {
                        mRunOnUiThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                GLTextureView.this.onFrameCaptured(bitmap);
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void onFrameCaptured(Bitmap bitmap) {
        if(mOnFrameCapturedCallback != null) {
            mOnFrameCapturedCallback.onFrameCaptured(bitmap);
        }
    }
}
