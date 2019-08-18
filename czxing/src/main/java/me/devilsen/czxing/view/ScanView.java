package me.devilsen.czxing.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

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
public class ScanView extends BarCoderView implements ScanBoxView.ScanBoxClickListener,
        BarcodeReader.ReadCodeListener {

    private static final int DARK_LIST_SIZE = 4;

    private boolean isStop;
    private boolean isDark;
    private ArrayDeque<Boolean> darkList;

    private BarcodeProcessor processor;

    public ScanView(Context context) {
        this(context, null);
    }

    public ScanView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScanBoxView.setScanBoxClickListener(this);
        processor = new BarcodeProcessor();
        processor.setReadCodeListener(this);

        darkList = new ArrayDeque<>(DARK_LIST_SIZE);
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
    }

    @Override
    public void onReadCodeResult(CodeResult result) {
        if (result == null) {
            return;
        }
        BarCodeUtil.d(result.toString());

        if (!TextUtils.isEmpty(result.getText()) && !isStop) {
            isStop = true;
            if (mScanListener != null) {
                mScanListener.onScanSuccess(result.getText());
            }
        } else if (result.getPoints() != null) {
            tryZoom(result);
        }
    }

    @Override
    public void onAnalysisBrightness(boolean isDark) {
        BarCodeUtil.d("isDark  " + isDark);

        darkList.addFirst(isDark);
        if (darkList.size()  > DARK_LIST_SIZE){
            darkList.removeLast();
        }

        int show = 0;
        for (Boolean dark : darkList) {
            if (dark) {
                show++;
            }
        }

        this.isDark = show >= DARK_LIST_SIZE;
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
