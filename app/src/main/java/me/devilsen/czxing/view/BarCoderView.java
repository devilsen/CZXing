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
    private static final int DEFAULT_ZOOM_SCALE = 3;

    protected int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    protected Camera mCamera;
    private CameraSurface mCameraSurface;

    protected ScanListener mScanListener;
    private ScanBoxView mScanBoxView;

    protected boolean mSpotAble = false;
    private long processLastTime;
    private int scanTimes;

    private ValueAnimator mAutoZoomAnimator;
    private long mLastAutoZoomTime = 0;

    // 上次环境亮度记录的时间戳
    private long mLastAmbientBrightnessRecordTime = System.currentTimeMillis();
    // 上次环境亮度记录的索引
    private int mAmbientBrightnessDarkIndex = 0;
    // 环境亮度历史记录的数组，255 是代表亮度最大值
    private static final long[] AMBIENT_BRIGHTNESS_DARK_LIST = new long[]{255, 255, 255, 255};
    // 环境亮度扫描间隔
    private static final int AMBIENT_BRIGHTNESS_WAIT_SCAN_TIME = 250;
    // 亮度低的阀值
    private static final int AMBIENT_BRIGHTNESS_DARK = 60;

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
        if (System.currentTimeMillis() - processLastTime < 200) {
            return;
        }
        processLastTime = System.currentTimeMillis();

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

            if (scanTimes % 5 == 0) {
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

    private void handleAmbientBrightness(byte[] data, Camera camera) {
        if (mCameraSurface == null || !mCameraSurface.isPreviewing()) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastAmbientBrightnessRecordTime < AMBIENT_BRIGHTNESS_WAIT_SCAN_TIME) {
            return;
        }
        mLastAmbientBrightnessRecordTime = currentTime;

        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        int width = previewSize.width;
        int height = previewSize.height;
        // 像素点的总亮度
        long pixelLightCount = 0L;
        // 像素点的总数
        long pixelCount = width * height;
        // 采集步长，因为没有必要每个像素点都采集，可以跨一段采集一个，减少计算负担，必须大于等于1。
        int step = 10;
        // data.length - allCount * 1.5f 的目的是判断图像格式是不是 YUV420 格式，只有是这种格式才相等
        //因为 int 整形与 float 浮点直接比较会出问题，所以这么比
        if (Math.abs(data.length - pixelCount * 1.5f) < 0.00001f) {
            for (int i = 0; i < pixelCount; i += step) {
                // 如果直接加是不行的，因为 data[i] 记录的是色值并不是数值，byte 的范围是 +127 到 —128，
                // 而亮度 FFFFFF 是 11111111 是 -127，所以这里需要先转为无符号 unsigned long 参考 Byte.toUnsignedLong()
                pixelLightCount += ((long) data[i]) & 0xffL;
            }
            // 平均亮度
            long cameraLight = pixelLightCount / (pixelCount / step);
            // 更新历史记录
            int lightSize = AMBIENT_BRIGHTNESS_DARK_LIST.length;
            AMBIENT_BRIGHTNESS_DARK_LIST[mAmbientBrightnessDarkIndex = mAmbientBrightnessDarkIndex % lightSize] = cameraLight;
            mAmbientBrightnessDarkIndex++;
            boolean isDarkEnv = true;
            // 判断在时间范围 AMBIENT_BRIGHTNESS_WAIT_SCAN_TIME * lightSize 内是不是亮度过暗
            for (long ambientBrightness : AMBIENT_BRIGHTNESS_DARK_LIST) {
                if (ambientBrightness > AMBIENT_BRIGHTNESS_DARK) {
                    isDarkEnv = false;
                    break;
                }
            }
            BarCodeUtil.d("摄像头环境亮度为：" + cameraLight);
            if (mScanListener != null) {
                mScanListener.onBrightnessChanged(isDarkEnv);
            }
        }
    }


    public void onDestroy() {
        closeCamera();
        mScanListener = null;
    }

}
