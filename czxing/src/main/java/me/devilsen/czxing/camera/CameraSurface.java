package me.devilsen.czxing.camera;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.SensorController;

/**
 * desc : CameraSurface
 * date : 12/7/20 4:05 PM
 *
 * @author : dongSen
 */
public abstract class CameraSurface extends SurfaceView implements SurfaceHolder.Callback,
        SensorController.CameraFocusListener {

    protected float mAspectRatio;
    protected Camera mCamera;
    protected Point mFocusCenter;
    protected boolean mPreviewing;
    protected boolean mZoomOutFlag;
    protected SurfacePreviewListener mScanListener;

    public CameraSurface(Context context) {
        this(context, null);
    }

    public CameraSurface(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraSurface(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be
     * measured based on the ratio calculated from the parameters.
     *
     * @param width  Camera resolution horizontal size
     * @param height Camera resolution vertical size
     */
    void setAspectRatio(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Size cannot be negative");
        }
        mAspectRatio = (float) width / height;
        getHolder().setFixedSize(width, height);
        requestLayout();
    }

    public void setScanBoxPoint(Point boxCenterPoint) {
        mFocusCenter = boxCenterPoint;
    }

    public boolean isPreviewing() {
        return mCamera != null && mPreviewing;
    }

    public void setCamera(Camera camera) {
        if (camera == null) {
            BarCodeUtil.d("Camera is null");
            return;
        }
        mCamera = camera;
        openCamera();
    }

    public abstract void openCamera();

    public abstract void startCameraPreview();

    public abstract void stopCameraPreview();

    public abstract void openFlashlight();

    public abstract void closeFlashlight();

    /**
     * 放大缩小
     *
     * @param isZoomIn true：缩小
     * @param scale    放大缩小的数值
     */
    public abstract void handleZoom(boolean isZoomIn, int scale);

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
