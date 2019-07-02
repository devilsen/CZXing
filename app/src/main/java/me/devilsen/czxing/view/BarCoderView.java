package me.devilsen.czxing.view;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import me.devilsen.czxing.camera.CameraSurface;
import me.devilsen.czxing.camera.CameraUtil;

/**
 * @author : dongSen
 * date : 2019-06-29 15:35
 * desc :
 */
abstract class BarCoderView extends FrameLayout implements Camera.PreviewCallback {

    private static final int NO_CAMERA_ID = -1;

    protected int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    protected Camera mCamera;
    private CameraSurface mCameraSurface;

    private ScanListener mScanListener;
    private ScanBoxView mScanBoxView;

    protected boolean mSpotAble = false;
    private long time;

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

        setOneShotPreviewCallback();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (System.currentTimeMillis() - time < 100) {
            return;
        }
        time = System.currentTimeMillis();

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

        onPreviewFrame(data, 0, 0, rowWidth, rowHeight, rowWidth);
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
        setOneShotPreviewCallback();
    }

    public void stopScan() {
        mSpotAble = false;

        if (mCamera == null) {
            return;
        }
        try {
//            mCamera.setOneShotPreviewCallback(this);
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
    private void setOneShotPreviewCallback() {
        if (mSpotAble && mCameraSurface.isPreviewing()) {
            try {
//            mCamera.setOneShotPreviewCallback(this);
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

    public void onDestroy() {
        closeCamera();
        mScanListener = null;
    }

}
