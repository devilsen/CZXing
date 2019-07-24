package me.devilsen.czxing.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import me.devilsen.czxing.BarcodeReader;
import me.devilsen.czxing.thread.Callback;
import me.devilsen.czxing.thread.Dispatcher;
import me.devilsen.czxing.util.BarCodeUtil;

/**
 * @author : dongSen
 * date : 2019-06-29 16:18
 * desc : 二维码界面使用类
 */
public class ScanView extends BarCoderView implements Callback, ScanBoxView.ScanBoxClickListener {

    private Dispatcher mDispatcher;
    private boolean isDark;
    private boolean isStop;

    public ScanView(Context context) {
        this(context, null);
    }

    public ScanView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDispatcher = new Dispatcher();
        mScanBoxView.setScanBoxClickListener(this);
    }

    @Override
    public void onPreviewFrame(byte[] data, int left, int top, int width, int height, int rowWidth) {
        if (isStop) {
            return;
        }
//        SaveImageUtil.saveData(data, left, top, width, height, rowWidth);
        int queueSize = mDispatcher.newRunnable(data, left, top, width, height, rowWidth, this).enqueue();
        setQueueSize(queueSize);
//        BarcodeReader.Result result = reader.read(data, left, top, width, height, rowWidth);
//
//        if (result != null) {
//            if (!TextUtils.isEmpty(result.getText())) {
//                Log.e("result", result.getText());
//            }
//
//            if (result.getPoints() != null) {
//                tryZoom(result);
//            }
//        }
    }

    @Override
    public void stopScan() {
        super.stopScan();
        isStop = true;
        mDispatcher.cancelAll();
    }

    @Override
    public void onDecodeComplete(BarcodeReader.Result result) {
        if (result == null) {
            return;
        }
        if (!TextUtils.isEmpty(result.getText())) {
            mDispatcher.cancelAll();
            isStop = true;
            if (mScanListener != null) {
                mScanListener.onScanSuccess(result.getText());
            }
        } else if (result.getPoints() != null) {
            tryZoom(result);
        }
    }

    @Override
    public void onDarkBrightness(boolean isDark) {
        this.isDark = isDark;
        BarCodeUtil.d("isDark  " + isDark);
        mScanBoxView.setDark(isDark);
    }

    @Override
    public void onFlashLightClick() {
        mCameraSurface.toggleFlashLight(isDark);
    }

    @Override
    public void onCardTextClick() {
        if (mScanListener != null) {
            mScanListener.onClickCard();
        }
    }

    public void hideCard() {
        mScanBoxView.hideCardText();
    }
}
