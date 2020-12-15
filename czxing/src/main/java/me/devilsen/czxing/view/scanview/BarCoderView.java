package me.devilsen.czxing.view.scanview;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import me.devilsen.czxing.camera.ScanCamera;
import me.devilsen.czxing.camera.camera1.ScanCamera1;
import me.devilsen.czxing.camera.camera2.ScanCamera2;
import me.devilsen.czxing.code.CodeResult;
import me.devilsen.czxing.compat.ContextCompat;
import me.devilsen.czxing.thread.ExecutorUtil;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.CameraUtil;
import me.devilsen.czxing.util.ResolutionAdapterUtil;
import me.devilsen.czxing.view.AutoFitSurfaceView;
import me.devilsen.czxing.view.resultview.ScanResultView;

import static me.devilsen.czxing.view.scanview.ScanView.SCAN_MODE_MIX;
import static me.devilsen.czxing.view.scanview.ScanView.SCAN_MODE_TINY;

/**
 * @author : dongSen
 * date : 2019-06-29 15:35
 * desc : 二维码扫描界面，包括Surface和Box
 */
abstract class BarCoderView extends FrameLayout implements ScanCamera.ScanPreviewCallback {

    private static final int DEFAULT_ZOOM_SCALE = 3;
    private static final long ONE_HUNDRED_MILLISECONDS = 100;

    protected ScanCamera mCamera;
    private AutoFitSurfaceView mCameraSurface;
    protected ScanBoxView mScanBoxView;
    private ScanResultView mScanResultView;

    protected boolean mSpotAble;
    private int scanSequence;

    protected ScanListener mScanListener;
    private ValueAnimator mAutoZoomAnimator;

    private long processLastTime;
    private long mLastAutoZoomTime;
    private ResolutionAdapterUtil resolutionAdapter;
    private int scanMode;

    public BarCoderView(Context context) {
        this(context, null);
    }

    public BarCoderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BarCoderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setBackground(null);
        mCameraSurface = new AutoFitSurfaceView(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mCamera = new ScanCamera2(context, mCameraSurface);
        } else {
            mCamera = new ScanCamera1(context, mCameraSurface);
        }
        mCamera.onCreate();
        mCamera.setPreviewListener(this);

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mCameraSurface, params);

        mScanBoxView = new ScanBoxView(context);
        addView(mScanBoxView, params);

        resolutionAdapter = new ResolutionAdapterUtil();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        resolutionAdapter.setResolutionSize(right - left, bottom - top);
        mCamera.setScanBoxPoint(mScanBoxView.getScanBoxCenter());
    }

    @Override
    public void onPreviewFrame(byte[] data, int rowWidth, int rowHeight) {
        long now = SystemClock.uptimeMillis();
        if ((now - processLastTime) < ONE_HUNDRED_MILLISECONDS) {
            return;
        }
        processLastTime = now;

        try {
            Rect scanBoxRect = mScanBoxView.getScanBoxRect();
            int scanBoxSize = mScanBoxView.getScanBoxSizeExpand();
            int expandTop = mScanBoxView.getExpandTop();

            int left;
            int top;
            // 这里需要把得到的数据也翻转
            boolean portrait = CameraUtil.isPortrait(getContext());
            if (portrait) {
                left = scanBoxRect.top - expandTop;
                top = scanBoxRect.left;
            } else {
                left = scanBoxRect.left;
                top = scanBoxRect.top;
            }

            resolutionAdapter.setCameraSize(portrait, rowWidth, rowHeight);
            left = resolutionAdapter.getAdapterWidth(left);
            top = resolutionAdapter.getAdapterHeight(top);
            scanBoxSize = resolutionAdapter.getAdapterWidth(scanBoxSize);
            scanDataStrategy(data, left, top, scanBoxSize, scanBoxSize, rowWidth, rowHeight);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 扫码识别策略
     */
    private void scanDataStrategy(byte[] data, int left, int top, int width, int height, int rowWidth, int rowHeight) {
        if (scanMode == SCAN_MODE_MIX) {
            if (scanSequence < 5) {
                onPreviewFrame(data, left, top, width, height, rowWidth, rowHeight);
            } else {
                scanSequence = -1;
                int bisSize = Math.min(rowWidth, rowHeight);
                onPreviewFrame(data, 0, top, bisSize, bisSize, rowWidth, rowHeight);
            }
            scanSequence++;
        } else if (scanMode == SCAN_MODE_TINY) {
            onPreviewFrame(data, left, top, width, height, rowWidth, rowHeight);
        } else {
            int bisSize = Math.min(rowWidth, rowHeight);
            onPreviewFrame(data, 0, top, bisSize, bisSize, rowWidth, rowHeight);
        }
    }

    public abstract void onPreviewFrame(byte[] data, int left, int top, int width, int height, int rowWidth, int rowHeight);

    public void setScanListener(ScanListener listener) {
        mScanListener = listener;
    }

    public void openCamera() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            mCamera.onCreate();
        }
    }

    public void closeCamera() {
        mCamera.onPause();
    }

    public void startScan() {
        mSpotAble = true;
        mScanBoxView.startAnim();
    }

    public void stopScan() {
        mSpotAble = false;
        mScanBoxView.stopAnim();
        mScanBoxView.turnOffLight();
    }

    /**
     * 停止识别，并且隐藏扫描框
     */
    public void stopSpotAndHiddenRect() {
        stopScan();
    }

    /**
     * 显示扫描框，并开始识别
     */
    public void startSpotAndShowRect() {
        startScan();
    }

    /**
     * 显示获取到的二维码位置
     *
     * @param result 二维码定位信息
     */
    void showCodeBorder(CodeResult result) {
        float[] points = result.getPoints();
        if (points.length > 3) {
            int left = 0;
            int top = 0;
            int right = 0;
            int bottom = 0;
            int scanType = result.getScanType();
            if (scanType == 0) { // 正常
                left = (int) points[0];
                top = (int) points[5];
                right = (int) points[4];
                bottom = (int) points[1];
            } else if (scanType == 1) { // 旋转90度
                left = (int) points[2];
                top = (int) points[3];
                right = (int) points[4];
                bottom = (int) points[5];
            } else if (scanType == 2 || scanType == 4) { // 旋转180度
                left = (int) points[2];
                top = (int) points[5];
                right = (int) points[0];
                bottom = (int) points[1];
            } else if (scanType == 3) { // 旋转270度
                left = (int) points[4];
                top = (int) points[5];
                right = (int) points[2];
                bottom = (int) points[3];
            }

            mScanBoxView.drawFocusRect(left, top, right, bottom);
        }
    }

    /**
     * 没有查询到二维码结果，但是能基本能定位到二维码，根据返回的数据集，检验是否要放大
     *
     * @param result 二维码定位信息
     */
    void tryZoom(CodeResult result) {
        int len = 0;
        float[] points = result.getPoints();
        if (points.length > 3) {
            float point1X = points[0];
            float point1Y = points[1];
            float point2X = points[2];
            float point2Y = points[3];
            float xLen = Math.abs(point1X - point2X);
            float yLen = Math.abs(point1Y - point2Y);
            len = (int) Math.sqrt(xLen * xLen + yLen * yLen);
        }

        if (points.length > 5) {
            float point2X = points[2];
            float point2Y = points[3];
            float point3X = points[4];
            float point3Y = points[5];
            float xLen = Math.abs(point2X - point3X);
            float yLen = Math.abs(point2Y - point3Y);
            int len2 = (int) Math.sqrt(xLen * xLen + yLen * yLen);
            if (len2 < len) {
                len = len2;
            }
        }

        handleAutoZoom(len);
    }

    private void handleAutoZoom(int len) {
        try {
            BarCodeUtil.d("len: " + len);

            if (mCamera == null || mScanBoxView == null || len <= 0 || mCamera.hadZoomOut()) {
                return;
            }

            int scanBoxWidth = mScanBoxView.getScanBoxSize();
            if (len > scanBoxWidth / DEFAULT_ZOOM_SCALE) {
                if (mAutoZoomAnimator != null && mAutoZoomAnimator.isRunning()) {
                    ExecutorUtil.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAutoZoomAnimator.cancel();
                        }
                    });
                }
                return;
            }

            if (mAutoZoomAnimator != null && mAutoZoomAnimator.isRunning()) {
                return;
            }

            if (SystemClock.uptimeMillis() - mLastAutoZoomTime < 220) {
                return;
            }

            if (!mCamera.isZoomSupported()) {
                return;
            }

            // 二维码在扫描框中的宽度小于扫描框的 1/4，放大镜头
            final int maxZoom = (int) mCamera.getMaxZoom();
            // 在一些低端机上放太大，可能会造成画面过于模糊，无法识别
            final int maxCanZoom = maxZoom / 2;
            final int zoomStep = maxZoom / 6;
            final int zoom = (int) mCamera.getZoom();
//        BarCodeUtil.d("maxZoom: " + maxZoom + " maxCanZoom:" + maxCanZoom + " current: " + zoom + " len:" + len);

            ExecutorUtil.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startAnimationZoom(zoom, Math.min(zoom + zoomStep, maxCanZoom));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startAnimationZoom(int oldZoom, int newZoom) {
        mAutoZoomAnimator = ValueAnimator.ofInt(oldZoom, newZoom);
        mAutoZoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mCameraSurface == null) {
                    return;
                }
                int zoom = (int) animation.getAnimatedValue();
                mCamera.zoom(zoom);
            }
        });
        mAutoZoomAnimator.setDuration(200);
        mAutoZoomAnimator.setInterpolator(new DecelerateInterpolator());
        mAutoZoomAnimator.start();
        mLastAutoZoomTime = SystemClock.uptimeMillis();
    }

    public void resetZoom() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            mCamera.zoom(0);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAutoZoomAnimator != null) {
            mAutoZoomAnimator.cancel();
        }
    }

    public void onDestroy() {
        if (mScanBoxView != null) {
            mScanBoxView.onDestroy();
        }
        mCamera.onDestroy();
        mScanListener = null;
    }

    /**
     * 扫描模式，提供了三种扫码模式
     * ┏--------------┓
     * ┃              ┃
     * ┃       2      ┃
     * ┃    ┏----┓    ┃
     * ┃    ┃  1 ┃    ┃
     * ┃    ┗----┛    ┃
     * ┃              ┃
     * ┃              ┃
     * ┃              ┃
     * ┃              ┃
     * ┗--------------┛
     * <p>
     * 0：混合扫描模式（默认），扫描4次扫码框里的内容，扫描1次以屏幕宽为边长的内容
     * 1：只扫描扫码框里的内容
     * 2：扫描以屏幕宽为边长的内容
     *
     * @param scanMode 0 / 1 / 2
     */
    public void setScanMode(int scanMode) {
        this.scanMode = scanMode;
    }

    /**
     * 打开闪光灯
     */
    public void openFlashlight() {
        if (mCamera != null) {
            mCamera.openFlashlight();
        }
    }

    /**
     * 关闭闪光灯
     */
    public void closeFlashlight() {
        if (mCamera != null) {
            mCamera.closeFlashlight();
        }
    }

    /**
     * 获取 CameraSurface
     *
     * @return CameraSurface
     */
    public AutoFitSurfaceView getCameraSurface() {
        return mCameraSurface;
    }

    public ScanBoxView getScanBox() {
        return mScanBoxView;
    }

}
