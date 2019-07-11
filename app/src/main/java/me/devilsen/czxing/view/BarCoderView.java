package me.devilsen.czxing.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import me.devilsen.czxing.BarCodeUtil;
import me.devilsen.czxing.camera.CameraSurface;
import me.devilsen.czxing.camera.CameraUtil;

/**
 * @author : dongSen
 * date : 2019-06-29 15:35
 * desc :
 */
abstract class BarCoderView extends FrameLayout implements Camera.PreviewCallback {

    private static final int NO_CAMERA_ID = -1;
    private static final int DEFAULT_ZOOM_SCALE = 4;

    protected int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    protected Camera mCamera;
    CameraSurface mCameraSurface;
    ScanBoxView mScanBoxView;

    protected ScanListener mScanListener;
    protected boolean mSpotAble = false;
    private long processLastTime;
    private int scanTimes;
    private ValueAnimator mAutoZoomAnimator;
    private long mLastAutoZoomTime = 0;

    public BarCoderView(Context context) {
        this(context, null);
    }

    public BarCoderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BarCoderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setBackground(null);
        mCameraSurface = new CameraSurface(context);

        FrameLayout.LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mCameraSurface, params);

        mScanBoxView = new ScanBoxView(context);
        addView(mScanBoxView, params);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mCameraSurface.setScanBoxPoint(mScanBoxView.getScanBoxCenter());
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        long now = System.nanoTime();
        if (Math.abs(now - processLastTime) < 200000000) {
            return;
        }
        processLastTime = now;

        try {
            Rect scanBoxRect = mScanBoxView.getScanBoxRect();
            int scanBoxSize = mScanBoxView.getScanBoxSize();
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size size = parameters.getPreviewSize();

            int left;
            int top;
            int rowWidth;
            int rowHeight;
            // 这里需要把得到的数据也翻转
            if (CameraUtil.isPortrait(getContext())) {
                left = scanBoxRect.top;
                top = scanBoxRect.left;
                rowWidth = size.width;
                rowHeight = size.height;
            } else {
                left = scanBoxRect.left;
                top = scanBoxRect.top;
                rowWidth = size.height;
                rowHeight = size.width;
            }
            // TODO 这里需要一个策略
            onPreviewFrame(data, left, top, scanBoxSize, scanBoxSize, rowWidth);

            if (scanTimes % 4 == 0) {
                onPreviewFrame(data, 0, 0, rowWidth, rowHeight, rowWidth);
            }
            scanTimes++;

            BarCodeUtil.d("scan sequence " + scanTimes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public abstract void onPreviewFrame(byte[] data, int left, int top, int width, int height, int rowWidth);

    public void setScanListener(ScanListener listener) {
        mCameraSurface.setScanListener(listener);
        mScanListener = listener;
    }

    public void openCamera() {
        openCamera(mCameraId);
    }

    public void startScan() {
        mSpotAble = true;
        openCamera();
        setPreviewCallback();
    }

    public void stopScan() {
        mSpotAble = false;

        if (mCamera == null) {
            return;
        }
        try {
//            mCamera.setPreviewCallback(this);
            mCamera.setPreviewCallback(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void openCamera(int cameraFacing) {
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
            mCameraSurface.setCamera(mCamera);
        } catch (Exception e) {
            e.printStackTrace();
            if (mScanListener != null) {
                mScanListener.onOpenCameraError();
            }
        }
    }

    /**
     * 添加摄像头获取图像数据的回调
     */
    private void setPreviewCallback() {
        if (mSpotAble && mCameraSurface.isPreviewing()) {
            try {
//            mCamera.setPreviewCallback(this);
                mCamera.setPreviewCallback(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭摄像头预览
     */
    public void closeCamera() {
        try {
            stopSpotAndHiddenRect();
            if (mCamera != null) {
                mCameraSurface.stopCameraPreview();
                mCameraSurface.setCamera(null);
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止识别，并且隐藏扫描框
     */
    public void stopSpotAndHiddenRect() {
        stopScan();
//        hiddenScanRect();
    }

    /**
     * 显示扫描框，并开始识别
     */
    public void startSpotAndShowRect() {
        startScan();
//        showScanRect();
    }

    void handleAutoZoom(int len) {
        if (mCamera == null || mScanBoxView == null) {
            return;
        }
        if (len <= 0) {
            return;
        }
        if (mAutoZoomAnimator != null && mAutoZoomAnimator.isRunning()) {
            return;
        }
        if (System.currentTimeMillis() - mLastAutoZoomTime < 1200) {
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        if (!parameters.isZoomSupported()) {
            return;
        }

        int scanBoxWidth = mScanBoxView.getScanBoxSize();
        if (len > scanBoxWidth / DEFAULT_ZOOM_SCALE) {
            return;
        }
        // 二维码在扫描框中的宽度小于扫描框的 1/4，放大镜头
        final int maxZoom = parameters.getMaxZoom();
        final int zoomStep = maxZoom / 4;
        final int zoom = parameters.getZoom();
        post(new Runnable() {
            @Override
            public void run() {
                startAutoZoom(zoom, Math.min(zoom + zoomStep, maxZoom));
            }
        });
    }


    private void startAutoZoom(int oldZoom, int newZoom) {
        mAutoZoomAnimator = ValueAnimator.ofInt(oldZoom, newZoom);
        mAutoZoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mCameraSurface == null || !mCameraSurface.isPreviewing()) {
                    return;
                }
                int zoom = (int) animation.getAnimatedValue();
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setZoom(zoom);
                mCamera.setParameters(parameters);
            }
        });
        mAutoZoomAnimator.setDuration(600);
        mAutoZoomAnimator.setRepeatCount(0);
        mAutoZoomAnimator.start();
        mLastAutoZoomTime = System.currentTimeMillis();
    }

    public void onDestroy() {
        closeCamera();
        mScanListener = null;
    }

}
