package me.devilsen.czxing.camera;

import android.content.Context;
import android.graphics.Point;

import me.devilsen.czxing.util.SensorController;
import me.devilsen.czxing.view.AutoFitSurfaceView;

/**
 * desc :
 * date : 12/9/20 11:44 AM
 *
 * @author : dongSen
 */
public abstract class ScanCamera implements SensorController.CameraFocusListener,AutoFitSurfaceView.OnTouchListener {

    protected Context mContext;
    protected AutoFitSurfaceView mSurfaceView;
    protected Point mFocusCenter;
    protected boolean mPreviewing;
    protected boolean mZoomOutFlag;
    protected ScanPreviewCallback mScanCallback;
    protected final SensorController mSensorController;

    public ScanCamera(Context context, AutoFitSurfaceView surfaceView) {
        this.mContext = context;
        this.mSurfaceView = surfaceView;

        surfaceView.setOnTouchListener(this);
        mSensorController = new SensorController(context);
        mSensorController.setCameraFocusListener(this);
    }

    public abstract void onCreate();

    public abstract void onResume();

    public abstract void onStop();

    public abstract void onDestroy();

    public void setScanBoxPoint(Point boxCenterPoint) {
        mFocusCenter = boxCenterPoint;
    }

    public boolean isPreviewing() {
        return mPreviewing;
    }

    public abstract void startCameraPreview();

    public abstract void stopCameraPreview();

    public abstract void openFlashlight();

    public abstract void closeFlashlight();

    public abstract void focus(int focusPointX, int focusPointY);

    /**
     * 放大缩小
     */
    public abstract int zoom(int zoomValue);

    /**
     * 是否有过缩小操作
     *
     * @return true：缩小过
     */
    public boolean hadZoomOut() {
        return mZoomOutFlag;
    }

    public void setPreviewListener(ScanPreviewCallback listener) {
        this.mScanCallback = listener;
    }

    public interface ScanPreviewCallback {
        void onPreviewFrame(byte[] data, int rowWidth, int rowHeight);
    }

}
