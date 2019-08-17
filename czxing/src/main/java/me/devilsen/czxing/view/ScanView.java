package me.devilsen.czxing.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import me.devilsen.czxing.code.BarcodeReader;
import me.devilsen.czxing.code.CodeResult;
import me.devilsen.czxing.processor.BarcodeProcessor;
import me.devilsen.czxing.thread.Callback;
import me.devilsen.czxing.thread.Dispatcher;
import me.devilsen.czxing.util.BarCodeUtil;

/**
 * @author : dongSen
 * date : 2019-06-29 16:18
 * desc : 二维码界面使用类
 */
public class ScanView extends BarCoderView implements Callback, ScanBoxView.ScanBoxClickListener,
        BarcodeReader.ReadCodeListener {

    private Dispatcher mDispatcher;
    private boolean isDark;
    private boolean isStop;

    private BarcodeProcessor processor;

    public ScanView(Context context) {
        this(context, null);
    }

    public ScanView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDispatcher = new Dispatcher();
        mScanBoxView.setScanBoxClickListener(this);
        processor = new BarcodeProcessor();
        processor.setReadCodeListener(this);
    }

    @Override
    public void onPreviewFrame(byte[] data, int left, int top, int width, int height, int rowWidth, int rowHeight) {
        if (isStop) {
            return;
        }

        processor.processBytes(data, left, top, width, height, rowWidth, rowHeight);
//        SaveImageUtil.saveData(data, left, top, width, height, rowWidth);
//        int queueSize = mDispatcher.newRunnable(data, left, top, width, height, rowWidth, rowHeight, this).enqueue();
//        setQueueSize(queueSize);

//        isStop = true;
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
    public void startScan() {
        super.startScan();
        isStop = false;
    }

    @Override
    public void stopScan() {
        super.stopScan();
        isStop = true;
        mDispatcher.cancelAll();
    }

    @Override
    public void onReadCodeResult(CodeResult result) {
        onDecodeComplete(result);
    }

    @Override
    public void onDecodeComplete(CodeResult result) {
        if (result == null) {
            return;
        }
        BarCodeUtil.d(result.toString());

        if (!TextUtils.isEmpty(result.getText()) && !isStop) {
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

    public void resetZoom() {
        setZoomValue(0);
    }

    @Override
    public void onFlashLightClick() {
        mCameraSurface.toggleFlashLight(isDark);
    }

}
