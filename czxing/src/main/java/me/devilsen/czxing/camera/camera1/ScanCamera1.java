package me.devilsen.czxing.camera.camera1;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Collections;

import me.devilsen.czxing.camera.ScanCamera;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.CameraUtil;
import me.devilsen.czxing.view.AutoFitSurfaceView;

/**
 * @author : dongSen
 * date : 2019-06-29 13:54
 * desc : Camera1 for low version android
 */
public class ScanCamera1 extends ScanCamera implements Camera.PreviewCallback {

    private static final int NO_CAMERA_ID = -1;

    private Camera mCamera;
    private Camera.Parameters mCameraParams;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private CameraConfigurationManager mCameraConfigurationManager;

    private boolean mPreviewing = true;
    private boolean mIsTouchFocusing;

    private float mZoomLevel;
    private Rect mPreviewRect;

    public ScanCamera1(Context context, AutoFitSurfaceView surfaceView) {
        super(context, surfaceView);
    }

    @Override
    public void onCreate() {
        openCamera();

        try {
            SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
            surfaceHolder.setKeepScreenOn(true);
            surfaceHolder.addCallback(mSurfaceHolderCallback);
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        openCamera();
        if (mCamera == null) {
            return;
        }
        mPreviewing = true;

        startCameraPreview();
        mSensorController.onStart();
    }

    @Override
    public void onPause() {
        mIsFlashLighting = false;
        mPreviewing = false;
        if (mCamera == null) {
            return;
        }
        mSensorController.onStop();
        stopCameraPreview();
        mCamera.release();
        mCamera = null;
    }

    @Override
    public void onDestroy() {
        if (mCamera != null) {
            mCamera.release();
        }
    }

    @Override
    public void addCallbackBuffer(byte[] data) {
        mCamera.addCallbackBuffer(data);
    }

    public void openCamera() {
        int cameraFacing = mCameraId;
        if (mCamera != null || Camera.getNumberOfCameras() == 0) {
            return;
        }
        int ultimateCameraId = findCameraIdByFacing(cameraFacing);
        if (ultimateCameraId != NO_CAMERA_ID) {
            startCameraById(ultimateCameraId);
            return;
        }

        if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            ultimateCameraId = findCameraIdByFacing(Camera.CameraInfo.CAMERA_FACING_FRONT);
        } else if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            ultimateCameraId = findCameraIdByFacing(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
        if (ultimateCameraId != NO_CAMERA_ID) {
            startCameraById(ultimateCameraId);
        }
    }

    private int findCameraIdByFacing(int cameraFacing) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int cameraId = 0; cameraId < Camera.getNumberOfCameras(); cameraId++) {
            try {
                Camera.getCameraInfo(cameraId, cameraInfo);
                if (cameraInfo.facing == cameraFacing) {
                    return cameraId;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return NO_CAMERA_ID;
    }

    private void startCameraById(int cameraId) {
        try {
            mCameraId = cameraId;
            mCamera = Camera.open(cameraId);
            setCamera(mCamera);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setCamera(Camera camera) {
        if (camera == null) {
            return;
        }
        this.mCamera = camera;

        mCameraConfigurationManager = new CameraConfigurationManager(mContext);
        mCameraConfigurationManager.initFromCameraParameters(mCamera);
        if (mPreviewing) {
            mSurfaceView.requestLayout();
        } else {
            startCameraPreview();
        }

        mSurfaceView.getHolder().addCallback(mSurfaceHolderCallback);
    }

    private final SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            setSurfaceSize();
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            if (holder.getSurface() == null) {
                return;
            }
            stopCameraPreview();
            startCameraPreview();
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            stopCameraPreview();
        }
    };

    private void setSurfaceSize() {
        int width = mSurfaceView.getWidth();
        int height = mSurfaceView.getHeight();
        BarCodeUtil.d("View finder size: " + mSurfaceView.getWidth() + " x " + mSurfaceView.getHeight());

        if (mCameraConfigurationManager != null && mCameraConfigurationManager.getCameraResolution() != null) {
            Point cameraResolution = mCameraConfigurationManager.getCameraResolution();
            // 取出来的cameraResolution高宽值与屏幕的高宽顺序是相反的
            int cameraPreviewWidth = cameraResolution.x;
            int cameraPreviewHeight = cameraResolution.y;
            if ((float) width / height < (float) cameraPreviewWidth / cameraPreviewHeight) {
                float ratio = (float) cameraPreviewHeight / cameraPreviewWidth;
                width = (int) (height / ratio);
            } else {
                float ratio = (float) cameraPreviewWidth / cameraPreviewHeight;
                height = (int) (width / ratio);
            }
        }
        BarCodeUtil.d("surface view size: " + width + " x " + height);
        mSurfaceView.setAspectRatio(height, width);
    }

    public void startCameraPreview() {
        if (mCamera == null) {
            return;
        }
        try {
            mPreviewing = true;
            SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
            surfaceHolder.setKeepScreenOn(true);
            surfaceHolder.addCallback(mSurfaceHolderCallback);
            mCamera.setPreviewDisplay(surfaceHolder);

            mCameraConfigurationManager.setDesiredCameraParameters(mCamera);
            mCamera.startPreview();
            mCamera.setPreviewCallback(this);
            startContinuousAutoFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopCameraPreview() {
        if (mCamera == null) {
            return;
        }
        try {
            mPreviewing = false;
            if (mSurfaceView.getHolder() != null) {
                mSurfaceView.getHolder().removeCallback(mSurfaceHolderCallback);
            }
            closeFlashlight();
            mCamera.cancelAutoFocus();
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mScanCallback != null) {
            mScanCallback.onPreviewFrame(data, getPreviewSize().width(), getPreviewSize().height());
        }
    }

    private boolean flashLightAvailable() {
        return isPreviewing() && mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    @Override
    public void openFlashlight() {
        if (flashLightAvailable()) {
            mIsFlashLighting = true;
            mCameraConfigurationManager.openFlashlight(mCamera);
        }
    }

    @Override
    public void closeFlashlight() {
        if (flashLightAvailable()) {
            mIsFlashLighting = false;
            mCameraConfigurationManager.closeFlashlight(mCamera);
        }
    }

    @Override
    public void focus(float focusPointX, float focusPointY) {
        BarCodeUtil.d("Focus x = " + focusPointX + " y = " + focusPointY);
        handleFocus(focusPointX, focusPointY);
    }

    @Override
    public boolean isZoomSupported() {
        return getParameters().isZoomSupported();
    }

    @Override
    public float getMaxZoom() {
        return getParameters().getMaxZoom();
    }

    @Override
    public float getZoom() {
        return getParameters().getZoom();
    }

    @Override
    public float zoom(float zoomLevel) {
        if (!isZoomSupported()) return 0;
        try {
            if (zoomLevel < 0) {
                return 0;
            }
            float maxZoom = getMaxZoom();
            if (zoomLevel > maxZoom) {
                return maxZoom;
            }

            Camera.Parameters params = getParameters();
            if (zoomLevel < mZoomLevel) {
                BarCodeUtil.d("缩小");
                mZoomOutFlag = true;
            }
            mZoomLevel = zoomLevel;
            params.setZoom((int) zoomLevel);
            mCamera.setParameters(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return zoomLevel;
    }

    private Camera.Parameters getParameters() {
        if (mCameraParams == null) {
            mCameraParams = mCamera.getParameters();
        }
        return mCameraParams;
    }

    public Rect getPreviewSize() {
        if (mPreviewRect == null) {
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            mPreviewRect = new Rect(0, 0, size.width, size.height);
        }
        return mPreviewRect;
    }

    /**
     * 双击放大
     */
    @Override
    public void doubleTap() {
        handleZoom(true, 5);
    }

    @Override
    public void touchZoom(float distance) {
        handleZoom(distance > 0, 1);
    }

    /**
     * 放大缩小
     *
     * @param isZoomIn true：缩小
     * @param scale    放大缩小的数值
     */
    void handleZoom(boolean isZoomIn, int scale) {
        try {
            Camera.Parameters params = mCamera.getParameters();
            if (params.isZoomSupported()) {
                int zoom = params.getZoom();
                if (isZoomIn && zoom < params.getMaxZoom()) {
                    BarCodeUtil.d("放大");
                    zoom += scale;
                } else if (!isZoomIn && zoom > 0) {
                    BarCodeUtil.d("缩小");
                    mZoomOutFlag = true;
                    zoom -= scale;
                } else {
                    BarCodeUtil.d("既不放大也不缩小");
                }
                params.setZoom(zoom);
                mCamera.setParameters(params);
            } else {
                BarCodeUtil.d("不支持缩放");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleFocus(float x, float y) {
        if (mIsTouchFocusing) return;
        mIsTouchFocusing = true;

        float centerX = x;
        float centerY = y;
        if (CameraUtil.isPortrait(mContext)) {
            float temp = centerX;
            centerX = centerY;
            centerY = temp;
        }
        int focusSize = CameraUtil.dp2px(mContext, 40);
        handleFocusMetering(centerX, centerY, focusSize, focusSize);
    }

    private void handleFocusMetering(float originFocusCenterX, float originFocusCenterY, int originFocusWidth, int originFocusHeight) {
        try {
            boolean isNeedUpdate = false;
            Camera.Parameters focusMeteringParameters = mCamera.getParameters();
            Camera.Size size = focusMeteringParameters.getPreviewSize();
            if (focusMeteringParameters.getMaxNumFocusAreas() > 0) {
                BarCodeUtil.d("支持设置对焦区域");
                isNeedUpdate = true;
                Rect focusRect = CameraUtil.calculateFocusMeteringArea(1f,
                        originFocusCenterX, originFocusCenterY,
                        originFocusWidth, originFocusHeight,
                        size.width, size.height);
                CameraUtil.printRect("对焦区域", focusRect);
                focusMeteringParameters.setFocusAreas(Collections.singletonList(new Camera.Area(focusRect, 1000)));
                focusMeteringParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            } else {
                BarCodeUtil.d("不支持设置对焦区域");
            }

            if (focusMeteringParameters.getMaxNumMeteringAreas() > 0) {
                BarCodeUtil.d("支持设置测光区域");
                isNeedUpdate = true;
                Rect meteringRect = CameraUtil.calculateFocusMeteringArea(1.5f,
                        originFocusCenterX, originFocusCenterY,
                        originFocusWidth, originFocusHeight,
                        size.width, size.height);
                CameraUtil.printRect("测光区域", meteringRect);
                focusMeteringParameters.setMeteringAreas(Collections.singletonList(new Camera.Area(meteringRect, 1000)));
            } else {
                BarCodeUtil.d("不支持设置测光区域");
            }

            if (isNeedUpdate) {
                mCamera.cancelAutoFocus();
                mCamera.setParameters(focusMeteringParameters);
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (success) {
                            BarCodeUtil.d("对焦测光成功");
                        } else {
                            BarCodeUtil.e("对焦测光失败");
                        }
                        startContinuousAutoFocus();
                    }
                });
            } else {
                mIsTouchFocusing = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            BarCodeUtil.e("对焦测光失败：" + e.getMessage());
            startContinuousAutoFocus();
        }
    }

    /**
     * 连续对焦
     */
    private void startContinuousAutoFocus() {
        mIsTouchFocusing = false;
        if (mCamera == null) {
            return;
        }

        try {
            Camera.Parameters parameters = mCamera.getParameters();
            // 连续对焦
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(parameters);
            // 要实现连续的自动对焦，这一句必须加上
            mCamera.cancelAutoFocus();
        } catch (Exception e) {
            BarCodeUtil.e("连续对焦失败");
        }
    }

    @Override
    public boolean isPreviewing() {
        return mCamera != null && mPreviewing;
    }

}
