package me.devilsen.czxing.camera;

import android.content.Context;
import android.graphics.Point;
import android.os.SystemClock;

import me.devilsen.czxing.view.AutoFitSurfaceView;

/**
 * desc :
 * date : 12/9/20 11:44 AM
 *
 * @author : dongSen
 */
public abstract class ScanCamera implements SensorController.CameraFocusListener,
        AutoFitSurfaceView.OnTouchListener {

    private static final long ONE_SECOND = 1000;

    protected final Context mContext;
    protected final AutoFitSurfaceView mSurfaceView;
    protected final SensorController mSensorController;

    private long mLastFrozenTime;
    protected Point mFocusCenter;
    protected boolean mZoomOutFlag;
    protected boolean mIsFlashLighting;
    protected boolean mPreviewing;

    protected ScanPreviewCallback mScanCallback;

    public ScanCamera(Context context, AutoFitSurfaceView surfaceView) {
        this.mContext = context;
        this.mSurfaceView = surfaceView;

        surfaceView.setOnTouchListener(this);
        mSensorController = new SensorController(context);
        mSensorController.setCameraFocusListener(this);
    }

    public abstract void onCreate();

    public abstract void onResume();

    public abstract void onPause();

    public abstract void onDestroy();

    public abstract void addCallbackBuffer(byte[] data);

    public void setScanBoxPoint(Point boxCenterPoint) {
        mFocusCenter = boxCenterPoint;
    }

    public boolean isPreviewing() {
        return mPreviewing;
    }

    public abstract void openFlashlight();

    public abstract void closeFlashlight();

    public boolean isFlashLighting() {
        return mIsFlashLighting;
    }

    public abstract void focus(float focusPointX, float focusPointY);

    /** 是否支持缩放 */
    public abstract boolean isZoomSupported();

    /** 最大缩放倍数 */
    public abstract float getMaxZoom();

    /** 获取当前缩放 */
    public abstract float getZoom();

    /** 缩放镜头 */
    public abstract float zoom(float zoomValue);

    /**
     * 是否有过缩小操作
     *
     * @return true：缩小过
     */
    public boolean hadZoomOut() {
        return mZoomOutFlag;
    }

    @Override
    public void touchFocus(float x, float y) {
        focus(x, y);
    }

    @Override
    public void onFrozen() {
        if (mSurfaceView == null || mSurfaceView.getWidth() == 0) return;
        long now = SystemClock.uptimeMillis();
        if (now - mLastFrozenTime < ONE_SECOND) {
            return;
        }
        mLastFrozenTime = now;

        int x, y;
        if (mFocusCenter != null) {
            x = mFocusCenter.x;
            y = mFocusCenter.y;
        } else {
            x = mSurfaceView.getWidth() / 2;
            y = mSurfaceView.getHeight() / 2;
        }

        focus(x, y);
    }

    public void setPreviewListener(ScanPreviewCallback listener) {
        this.mScanCallback = listener;
    }

    public interface ScanPreviewCallback {
        void onPreviewFrame(byte[] data, int rowWidth, int rowHeight);
    }

}
