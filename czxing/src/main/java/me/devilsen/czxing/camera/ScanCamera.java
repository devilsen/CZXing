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
public abstract class ScanCamera implements SensorController.CameraFocusListener {

    protected Context mContext;
    protected AutoFitSurfaceView mSurfaceView;
    protected Point mFocusCenter;
    protected boolean mPreviewing;
    protected boolean mZoomOutFlag;
    protected SurfacePreviewListener mScanListener;

    public ScanCamera(Context context, AutoFitSurfaceView surfaceView) {
        this.mContext = context;
        this.mSurfaceView = surfaceView;
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


    public void setPreviewListener(SurfacePreviewListener listener) {
        this.mScanListener = listener;
    }

    public interface SurfacePreviewListener {
        void onStartPreview();
    }

}
