package me.devilsen.czxing.view.scanview;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;

import me.devilsen.czxing.camera.ScanCamera;
import me.devilsen.czxing.camera.camera1.ScanCamera1;
import me.devilsen.czxing.thread.ExecutorUtil;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.ResolutionAdapter;
import me.devilsen.czxing.view.AutoFitSurfaceView;

/**
 * @author : dongSen
 * date : 2019-06-29 15:35
 * desc : 二维码扫描界面，包括Surface和Box
 */
public class CameraView extends FrameLayout implements ScanCamera.ScanPreviewCallback {

    protected ScanCamera mCamera;
    private AutoFitSurfaceView mCameraSurface;
    private ResolutionAdapter mResolutionAdapter;

    private ValueAnimator mAutoZoomAnimator;

    private long mLastAutoZoomTime;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setBackground(null);
        mCameraSurface = new AutoFitSurfaceView(context);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            mCamera = new ScanCamera2(context, mCameraSurface);
//        } else {
            mCamera = new ScanCamera1(context, mCameraSurface);
//        }
        mCamera.onCreate();
        mCamera.setPreviewListener(this);

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mCameraSurface, params);

        mResolutionAdapter = new ResolutionAdapter();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mResolutionAdapter.setResolutionSize(right - left, bottom - top);
    }

    @Override
    public void onPreviewFrame(byte[] data, int rowWidth, int rowHeight) {

    }

    public void openCamera() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            mCamera.onCreate();
        }
    }

    public void closeCamera() {
        mCamera.onPause();
    }

    public void stopPreview() {
        mCamera.onPause();
    }

    /**
     * 没有查询到二维码结果，但是能基本能定位到二维码，根据返回的数据集，检验是否要放大
     *
     * @param points 二维码定位信息
     */
    void tryZoom(int[] points) {
        int len = 0;
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

        try {
            BarCodeUtil.d("len: " + len);

            if (mCamera == null || len <= 0 || mCamera.hadZoomOut()) {
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

            ExecutorUtil.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startZoomAnimation(zoom, Math.min(zoom + zoomStep, maxCanZoom));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startZoomAnimation(int oldZoom, int newZoom) {
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
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
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
        if (mCamera != null) {
            mCamera.onDestroy();
            mCamera = null;
        }
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

}
