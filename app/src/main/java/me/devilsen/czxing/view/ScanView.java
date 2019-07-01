package me.devilsen.czxing.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import me.devilsen.czxing.SaveImageUtil;
import me.devilsen.czxing.thread.Dispatcher;

/**
 * @author : dongSen
 * date : 2019-06-29 16:18
 * desc :
 */
public class ScanView extends BarCoderView {

    private Dispatcher mDispatcher;

    public ScanView(Context context) {
        this(context, null);
    }

    public ScanView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mDispatcher = new Dispatcher();
    }

    @Override
    public void onPreviewFrame(byte[] data, int left, int top, int width, int height, int rowWidth) {
        SaveImageUtil.byteArray2Bitmap(data, left, top, width, height, rowWidth);
        mDispatcher.newRunnable(data, left, top, width, height, rowWidth).enqueue();
    }

    @Override
    public void stopScan() {
        super.stopScan();
        mDispatcher.cancelAll();
    }

}
