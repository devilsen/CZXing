package me.devilsen.czxing.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import me.devilsen.czxing.processor.CameraDataProcessor;

/**
 * @author : dongSen
 * date : 2019-06-29 16:18
 * desc :
 */
public class ScanView extends BarCoderView {

    private CameraDataProcessor mCameraDataProcessor;

    public ScanView(Context context) {
        this(context, null);
    }

    public ScanView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mCameraDataProcessor = new CameraDataProcessor();
    }
}
