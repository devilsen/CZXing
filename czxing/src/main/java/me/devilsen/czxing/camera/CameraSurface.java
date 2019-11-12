package me.devilsen.czxing.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Collections;

import me.devilsen.czxing.util.BarCodeUtil;

/**
 * @author : dongSen
 * date : 2019-06-29 13:54
 * desc : 摄像头预览画面
 */
public class CameraSurface extends SurfaceView implements SensorController.CameraFocusListener,
        SurfaceHolder.Callback {

    private static final long ONE_SECOND = 1000;
    private Camera mCamera;

    private float mOldDist = 1f;
    private boolean mPreviewing = true;
    private boolean mIsTouchFocusing;
    private boolean mSurfaceCreated;
    private boolean mFlashLightIsOpen;
    private boolean mZoomOutFlag;

    private Point focusCenter;
    private long mLastFrozenTime;
    private long mLastTouchTime;
    private SensorController mSensorController;
    private CameraConfigurationManager mCameraConfigurationManager;
    private SurfacePreviewListener scanListener;

    public CameraSurface(Context context) {
        this(context, null);
    }

    public CameraSurface(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraSurface(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mSensorController = new SensorController(context);
        mSensorController.setCameraFocusListener(this);
    }

    public void setCamera(Camera camera) {
        if (camera == null) {
            return;
        }
        this.mCamera = camera;

        mCameraConfigurationManager = new CameraConfigurationManager(getContext());
        mCameraConfigurationManager.initFromCameraParameters(mCamera);
        getHolder().addCallback(this);
        if (mPreviewing) {
            requestLayout();
        } else {
            startCameraPreview();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceCreated = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() == null) {
            return;
        }
        stopCameraPreview();
        startCameraPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceCreated = false;
        stopCameraPreview();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
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
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isPreviewing()) {
            return super.onTouchEvent(event);
        }

        if (event.getPointerCount() == 1) {
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            if (action == MotionEvent.ACTION_DOWN) {
                long now = System.currentTimeMillis();
                if (now - mLastTouchTime < 300) {
                    doubleTap();
                    mLastTouchTime = 0;
                    return true;
                }
                mLastTouchTime = now;
            } else if (action == MotionEvent.ACTION_UP) {
                if (mIsTouchFocusing) {
                    return true;
                }
                mIsTouchFocusing = true;
                handleFocus(event.getX(), event.getY());
                BarCodeUtil.d("手指触摸，触发对焦测光");
            }
        } else if (event.getPointerCount() == 2) {
            handleZoom(event);
        }
        return true;
    }

    public void startCameraPreview() {
        if (mCamera == null) {
            return;
        }
        try {
            mPreviewing = true;
            SurfaceHolder surfaceHolder = getHolder();
            surfaceHolder.setKeepScreenOn(true);
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
            mFlashLightIsOpen = false;
        } else if (isDark) {
            openFlashlight();
            mFlashLightIsOpen = true;
        }
    }


    public void openFlashlight() {
        if (flashLightAvailable()) {
            mCameraConfigurationManager.openFlashlight(mCamera);
        }
    }

    public void closeFlashlight() {
        if (flashLightAvailable()) {
            mCameraConfigurationManager.closeFlashlight(mCamera);
        }
    }

    private boolean flashLightAvailable() {
        return isPreviewing() && getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    /**
     * 双击放大
     */
    private void doubleTap() {
        handleZoom(true, 5);
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

        BarCodeUtil.d("mCamera is frozen, start focus x = " + focusCenter.x + " y = " + focusCenter.y);
        handleFocus(focusCenter.x, focusCenter.y);
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
        return mCamera != null && mPreviewing && mSurfaceCreated;
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
        if (CameraUtil.isPortrait(getContext())) {
            float temp = centerX;
            centerX = centerY;
            centerY = temp;
        }
        int focusSize = CameraUtil.dp2px(getContext(), 120);
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

    public void setScanBoxPoint(Point scanBoxCenter) {
        if (focusCenter == null) {
            focusCenter = scanBoxCenter;
        }
    }

    public interface SurfacePreviewListener {
        void onStartPreview();
    }

}
