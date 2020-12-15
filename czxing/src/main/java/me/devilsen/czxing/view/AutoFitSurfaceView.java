package me.devilsen.czxing.view;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;

import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.CameraUtil;

/**
 * desc : CameraSurfaceView
 * date : 12/7/20 4:05 PM
 *
 * @author : dongSen
 */
public class AutoFitSurfaceView extends SurfaceView {

    protected float mAspectRatio;
    private long mLastTouchTime;
    private float mOldDist = 1f;
    private OnTouchListener mOnTouchListener;

    public AutoFitSurfaceView(Context context) {
        this(context, null);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be
     * measured based on the ratio calculated from the parameters.
     *
     * @param width  Camera resolution horizontal size
     * @param height Camera resolution vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Size cannot be negative");
        }
        mAspectRatio = (float) width / height;
        getHolder().setFixedSize(width, height);
        requestLayout();
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
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            if (action == MotionEvent.ACTION_DOWN) {
                long now = SystemClock.uptimeMillis();
                if (now - mLastTouchTime < 300) {
                    if (mOnTouchListener != null) {
                        mOnTouchListener.doubleTap();
                    }
                    mLastTouchTime = 0;
                    return true;
                }
                mLastTouchTime = now;
            } else if (action == MotionEvent.ACTION_UP) {
                if (mOnTouchListener != null) {
                    mOnTouchListener.touchFocus(event.getX(), event.getY());
                }
                BarCodeUtil.d("手指触摸，触发对焦测光");
            }
        } else if (event.getPointerCount() == 2) {
            handleZoom(event);
        }
        return true;
    }

    private void handleZoom(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mOldDist = CameraUtil.calculateFingerSpacing(event);
                break;
            case MotionEvent.ACTION_MOVE:
                float newDist = CameraUtil.calculateFingerSpacing(event);
                float distance = newDist - mOldDist;
                if (mOnTouchListener != null && distance != 0) {
                    mOnTouchListener.touchZoom(distance);
                }
                break;
        }
    }

    public void setOnTouchListener(OnTouchListener listener) {
        mOnTouchListener = listener;
    }

    public interface OnTouchListener {
        void doubleTap();

        void touchFocus(float x, float y);

        void touchZoom(float distance);
    }

}
