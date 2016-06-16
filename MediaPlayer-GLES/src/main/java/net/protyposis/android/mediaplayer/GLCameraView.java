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

package net.protyposis.android.mediaplayer;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

/**
 * @author Mario Guggenberger
 */
public class GLCameraView extends GLTextureView {

    private static final String TAG = GLCameraView.class.getSimpleName();

    private SurfaceTexture mSurfaceTexture;
    private Camera mCamera;
    private int mCameraId;

    public GLCameraView(Context context) {
        super(context);
        init(context);
    }

    public GLCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mCameraId = 0;
        if(!checkCameraHardware(context)) {
            Log.w(TAG, "no camera present");
        }
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if(isInEditMode()) {
            // there's no camera in the layout editor available
            return false;
        }
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    @Override
    public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
        startCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopCamera();
        super.surfaceDestroyed(holder);
    }

    @Override
    public void onPause() {
        stopCamera();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void startCamera() {
        try {
            if(mCamera == null) {
                mCamera = Camera.open(mCameraId);

                // enable autofocus if available
                List<String> supportedFocusModes = mCamera.getParameters().getSupportedFocusModes();
                if(supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    Camera.Parameters params = mCamera.getParameters();
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    mCamera.setParameters(params);
                }

                // set orientation
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(mCameraId, info);
                WindowManager windowManager = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
                int rotation = windowManager.getDefaultDisplay().getRotation();
                int degrees = 0;
                switch (rotation) {
                    case Surface.ROTATION_0: degrees = 0; break;
                    case Surface.ROTATION_90: degrees = 90; break;
                    case Surface.ROTATION_180: degrees = 180; break;
                    case Surface.ROTATION_270: degrees = 270; break;
                }
                int result;
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    result = (info.orientation + degrees) % 360;
                    result = (360 - result) % 360;
                } else {
                    result = (info.orientation - degrees + 360) % 360;
                }
                mCamera.setDisplayOrientation(result);

                // setup preview
                mCamera.setPreviewTexture(mSurfaceTexture);
                mCamera.startPreview();

                if(result == 0 || result == 180) {
                    mVideoWidth = mCamera.getParameters().getPreviewSize().width;
                    mVideoHeight = mCamera.getParameters().getPreviewSize().height;
                } else {
                    // swap width/height in portrait mode for a correct aspect ratio
                    mVideoHeight = mCamera.getParameters().getPreviewSize().width;
                    mVideoWidth = mCamera.getParameters().getPreviewSize().height;
                }

                getHolder().setFixedSize(mVideoWidth, mVideoHeight);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopCamera() {
        if(mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public boolean supportsCameraSwitch() {
        return Camera.getNumberOfCameras() > 1;
    }

    public void switchCamera() {
        mCameraId = ((mCameraId + 1) % Camera.getNumberOfCameras());
        stopCamera();
        startCamera();
    }
}
