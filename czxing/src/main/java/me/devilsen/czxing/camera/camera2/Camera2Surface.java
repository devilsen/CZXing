package me.devilsen.czxing.camera.camera2;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import androidx.annotation.RequiresApi;

import me.devilsen.czxing.camera.CameraSurface;
import me.devilsen.czxing.util.BarCodeUtil;

/**
 * desc :
 * date : 12/7/20 4:06 PM
 *
 * @author : dongSen
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Surface extends CameraSurface {

    private final CameraManager mCameraManager;

    public Camera2Surface(Context context) {
        this(context, null);
    }

    public Camera2Surface(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Camera2Surface(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (mAspectRatio == 0f) {
            setMeasuredDimension(width, height);
        } else {
            // Performs center-crop transformation of the camera frames
            int newWidth;
            int newHeight;
            float actualRatio;
            if (width > height) {
                actualRatio = mAspectRatio;
            } else {
                actualRatio = 1f / mAspectRatio;
            }
            if (width < height * actualRatio) {
                newHeight = height;
                newWidth = (int) (height * actualRatio);
            } else {
                newWidth = width;
                newHeight = (int) (width / actualRatio);
            }

            BarCodeUtil.d("Measured dimensions set: " + newWidth + " x " + newHeight);
            setMeasuredDimension(newWidth, newHeight);
        }
    }

    @Override
    public void openCamera() {

    }

    @Override
    public void startCameraPreview() {

    }

    @Override
    public void stopCameraPreview() {

    }

    @Override
    public void openFlashlight() {

    }

    @Override
    public void closeFlashlight() {

    }

    @Override
    public void handleZoom(boolean isZoomIn, int scale) {

    }

    @Override
    public void onFrozen() {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
