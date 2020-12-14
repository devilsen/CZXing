package me.devilsen.czxing.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

import me.devilsen.czxing.util.BarCodeUtil;

/**
 * desc : CameraSurfaceView
 * date : 12/7/20 4:05 PM
 *
 * @author : dongSen
 */
public class AutoFitSurfaceView extends SurfaceView {

    protected float mAspectRatio;

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

}
