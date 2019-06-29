package me.devilsen.czxing.camera;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;

import me.devilsen.czxing.BarCodeUtil;

/**
 * @author : dongSen
 * date : 2019-06-29 13:54
 * desc : 摄像头预览画面
 */
public class CameraSurface extends SurfaceView implements ICamera {

    // TODO camera2
    private CameraHelper mHelper;

    private float mOldDist = 1f;
    private boolean mIsTouchFocusing = false;

    public CameraSurface(Context context) {
        this(context, null);
    }

    public CameraSurface(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraSurface(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHelper = new CameraHelper(getContext());

    }

    public void setCamera(Camera camera) {
        mHelper.setCamera(camera, getHolder());
        if (mHelper.isPreviewing()) {
            requestLayout();
        } else {
            startCameraPreview();
        }
    }

    @Override
    public void startCameraPreview() {
        mHelper.startCameraPreview();
    }

    @Override
    public void stopCameraPreview() {
        mHelper.stopCameraPreview();
    }


    @Override
    public void openFlashlight() {
        mHelper.startCameraPreview();
    }

    @Override
    public void closeFlashlight() {
        mHelper.closeFlashlight();
    }

    @Override
    public void zoomIn() {
        mHelper.zoomIn();
    }

    @Override
    public void zoomOut() {
        mHelper.zoomOut();
    }

    @Override
    public void handleFocusMetering(float originFocusCenterX, float originFocusCenterY, int originFocusWidth, int originFocusHeight) {
        mHelper.handleFocusMetering(originFocusCenterX, originFocusCenterY, originFocusWidth, originFocusHeight);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        if (mHelper != null && mHelper.getCameraResolution() != null) {
            Point cameraResolution = mHelper.getCameraResolution();
            // 取出来的cameraResolution高宽值与屏幕的高宽顺序是相反的
            int cameraPreviewWidth = cameraResolution.x;
            int cameraPreviewHeight = cameraResolution.y;
            if (width * 1f / height < cameraPreviewWidth * 1f / cameraPreviewHeight) {
                float ratio = cameraPreviewHeight * 1f / cameraPreviewWidth;
                width = (int) (height / ratio + 0.5f);
            } else {
                float ratio = cameraPreviewWidth * 1f / cameraPreviewHeight;
                height = (int) (width / ratio + 0.5f);
            }
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mHelper == null || !mHelper.isPreviewing()) {
            return super.onTouchEvent(event);
        }

        if (event.getPointerCount() == 1 && (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            if (mIsTouchFocusing) {
                return true;
            }
            mIsTouchFocusing = true;
            handleFocus(event);
            mIsTouchFocusing = false;
        }

        if (event.getPointerCount() == 2) {
            handleZoom(event);
        }
        return true;
    }

    private void handleFocus(MotionEvent event) {
        BarCodeUtil.d("手指触摸触发对焦测光");
        float centerX = event.getX();
        float centerY = event.getY();
        if (CameraUtil.isPortrait(getContext())) {
            float temp = centerX;
            centerX = centerY;
            centerY = temp;
        }
        int focusSize = CameraUtil.dp2px(getContext(), 120);
        handleFocusMetering(centerX, centerY, focusSize, focusSize);
    }

    private void handleZoom(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mOldDist = CameraUtil.calculateFingerSpacing(event);
                break;
            case MotionEvent.ACTION_MOVE:
                float newDist = CameraUtil.calculateFingerSpacing(event);
                if (newDist > mOldDist) {
                    zoomIn();
                } else if (newDist < mOldDist) {
                    zoomOut();
                }
                break;
        }
    }

    public boolean isPreviewing() {
        return mHelper.isPreviewing();
    }

}
