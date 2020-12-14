package me.devilsen.czxing.camera.camera1;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Collections;

import me.devilsen.czxing.camera.ScanCamera;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.CameraUtil;
import me.devilsen.czxing.util.SensorController;
import me.devilsen.czxing.view.AutoFitSurfaceView;

/**
 * @author : dongSen
 * date : 2019-06-29 13:54
 * desc : Camera1 for low version android
 */
public class ScanCamera1 extends ScanCamera {

    private static final int NO_CAMERA_ID = -1;
    private static final long ONE_SECOND = 1000;
    private Camera mCamera;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    private float mOldDist = 1f;
    private boolean mPreviewing = true;
    private boolean mIsTouchFocusing;
    private boolean mSurfaceCreated;
    private boolean mFlashLightIsOpen;
    private boolean mZoomOutFlag;

    private Point focusCenter;
    private long mLastFrozenTime;
    private long mLastTouchTime;
    private CameraConfigurationManager mCameraConfigurationManager;
    private SurfacePreviewListener scanListener;

    public ScanCamera1(Context context, AutoFitSurfaceView surfaceView) {
        super(context, surfaceView);
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
            if (mScanListener != null) {
                mScanListener.onOpenCameraError();
            }
        }
    }

    public void setCamera(Camera camera) {
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

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                mSurfaceCreated = true;

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
                mSurfaceCreated = false;
                stopCameraPreview();
            }
        });
    }

    private void setSurfaceSize() {
        int width = mSurfaceView.getWidth();
        int height = mSurfaceView.getHeight();
        BarCodeUtil.d("View finder size: " + mSurfaceView.getWidth() + " x " + mSurfaceView.getHeight());

        if (mCameraConfigurationManager != null && mCameraConfigurationManager.getCameraResolution() != null) {
            Point cameraResolution = mCameraConfigurationManager.getCameraResolution();
            // 取出来的cameraResolution高宽值与屏幕的高宽顺序是相反的
            int cameraPreviewWidth = cameraResolution.x;
            int cameraPreviewHeight = cameraResolution.y;
            if (width * 1f / height < cameraPreviewWidth * 1f / cameraPreviewHeight) {
                float ratio = cameraPreviewHeight * 1f / cameraPreviewWidth;
                width = (int) (height / ratio + 0.5f);
            } else {
                float ratio = cameraPreviewWidth * 1f / cameraPreviewHeight;
                height = (int) (width / ratio + 0.5f);
            }
        }
        mSurfaceView.setAspectRatio(width, height);
    }

    public void startCameraPreview() {
        if (mCamera == null) {
            return;
        }
        try {
            mPreviewing = true;
            SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
            surfaceHolder.setKeepScreenOn(true);
            surfaceHolder.addCallback(this);
            mCamera.setPreviewDisplay(surfaceHolder);

            mCameraConfigurationManager.setDesiredCameraParameters(mCamera);
            mCamera.startPreview();
            if (scanListener != null) {
                scanListener.onStartPreview();
            }
            startContinuousAutoFocus();
            mSensorController.onStart();
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
                mSurfaceView.getHolder().removeCallback(this);
            }
            mCamera.cancelAutoFocus();
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mSensorController.onStop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void toggleFlashLight(boolean isDark) {
        if (mFlashLightIsOpen) {
            closeFlashlight();
        } else if (isDark) {
            openFlashlight();
        }
    }


    public void openFlashlight() {
        mFlashLightIsOpen = true;
        if (flashLightAvailable()) {
            mCameraConfigurationManager.openFlashlight(mCamera);
        }
    }

    public void closeFlashlight() {
        mFlashLightIsOpen = false;
        if (flashLightAvailable()) {
            mCameraConfigurationManager.closeFlashlight(mCamera);
        }
    }

    @Override
    public void focus(int focusPointX, int focusPointY) {

    }

    @Override
    public int zoom(int zoomValue) {
        return 0;
    }

    private boolean flashLightAvailable() {
        return isPreviewing() && mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    /**
     * 双击放大
     */
    public void doubleTap() {
        handleZoom(true, 5);
    }

    @Override
    public void touchFocus(float x, float y) {
        // TODO mIsTouchFocusing。 focus 需要时间，要加判断
    }

    @Override
    public void zoom(float distance) {

    }

    /**
     * 不动时对焦
     */
    @Override
    public void onFrozen() {
        long now = System.currentTimeMillis();
        if (now - mLastFrozenTime < ONE_SECOND) {
            return;
        }
        mLastFrozenTime = now;

        if (focusCenter != null) {
            BarCodeUtil.d("mCamera is frozen, start focus x = " + focusCenter.x + " y = " + focusCenter.y);
            handleFocus(focusCenter.x, focusCenter.y);
        }
    }

    /**
     * 放大缩小
     *
     * @param isZoomIn true：缩小
     */
    void handleZoom(boolean isZoomIn) {
        handleZoom(isZoomIn, 1);
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

    void handleFocusMetering(float originFocusCenterX, float originFocusCenterY, int originFocusWidth, int originFocusHeight) {
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

    public boolean isPreviewing() {
        return mCamera != null && mPreviewing;
    }

    /**
     * 是否有过缩小操作
     *
     * @return true：缩小过
     */
    public boolean hadZoomOut() {
        return mZoomOutFlag;
    }

    private void handleFocus(float x, float y) {
        float centerX = x;
        float centerY = y;
        if (CameraUtil.isPortrait(mContext)) {
            float temp = centerX;
            centerX = centerY;
            centerY = temp;
        }
        int focusSize = CameraUtil.dp2px(mContext, 80);
        handleFocusMetering(centerX, centerY, focusSize, focusSize);
    }

    private void handleZoom(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mOldDist = CameraUtil.calculateFingerSpacing(event);
                break;
            case MotionEvent.ACTION_MOVE:
                float newDist = CameraUtil.calculateFingerSpacing(event);
                if (newDist > mOldDist) {
                    handleZoom(true);
                } else if (newDist < mOldDist) {
                    handleZoom(false);
                }
                break;
        }
    }

    public void setPreviewListener(SurfacePreviewListener listener) {
        this.scanListener = listener;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void onDestroy() {

    }

    public void setScanBoxPoint(Point scanBoxCenter) {
        if (focusCenter == null) {
            focusCenter = scanBoxCenter;
        }
    }

    public interface SurfacePreviewListener {
        void onStartPreview();
    }

}
