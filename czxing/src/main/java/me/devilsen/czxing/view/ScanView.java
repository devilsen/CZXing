package me.devilsen.czxing.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import java.util.ArrayDeque;

import me.devilsen.czxing.code.BarcodeFormat;
import me.devilsen.czxing.code.BarcodeReader;
import me.devilsen.czxing.code.CodeResult;
import me.devilsen.czxing.util.BarCodeUtil;

/**
 * @author : dongSen
 * date : 2019-06-29 16:18
 * desc : 二维码界面使用类
 */
public class ScanView extends BarCoderView implements ScanBoxView.ScanBoxClickListener,
        BarcodeReader.ReadCodeListener {

    /**
     * 混合扫描模式（默认），扫描4次扫码框里的内容，扫描1次以屏幕宽为边长的内容
     */
    public static final int SCAN_MODE_MIX = 0;
    /**
     * 只扫描扫码框里的内容
     */
    public static final int SCAN_MODE_TINY = 1;
    /**
     * 扫描以屏幕宽为边长的内容
     */
    public static final int SCAN_MODE_BIG = 2;

    private static final int DARK_LIST_SIZE = 4;

    private boolean isStop;
    private boolean isDark;
    private ArrayDeque<Boolean> darkList;
    private BarcodeReader reader;

    public ScanView(Context context) {
        this(context, null);
    }

    public ScanView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScanBoxView.setScanBoxClickListener(this);
        reader = BarcodeReader.getInstance();
        reader.setBarcodeFormat(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.CODABAR,
                BarcodeFormat.CODE_128,
                BarcodeFormat.EAN_13,
                BarcodeFormat.UPC_A
        );
        reader.setReadCodeListener(this);

        darkList = new ArrayDeque<>(DARK_LIST_SIZE);
    }

    @Override
    public void onPreviewFrame(byte[] data, int left, int top, int width, int height, int rowWidth, int rowHeight) {
        if (isStop) {
            return;
        }

        reader.read(data, left, top, width, height, rowWidth, rowHeight);
//        SaveImageUtil.saveData(data, left, top, width, height, rowWidth);
//        int queueSize = mDispatcher.newRunnable(data, left, top, width, height, rowWidth, rowHeight, this).enqueue();
//        setQueueSize(queueSize);
    }

    @Override
    public void startScan() {
        super.startScan();
        reader.prepareRead();
        isStop = false;
    }

    @Override
    public void stopScan() {
        super.stopScan();
        reader.stopRead();
        isStop = true;
    }

    @Override
    public void onReadCodeResult(CodeResult result) {
        if (result == null) {
            return;
        }
        BarCodeUtil.d("result : " + result.toString());

        if (!TextUtils.isEmpty(result.getText()) && !isStop) {
            isStop = true;
            reader.stopRead();
            if (mScanListener != null) {
                mScanListener.onScanSuccess(result.getText());
            }
        } else if (result.getPoints() != null) {
            tryZoom(result);
        }
    }

    @Override
    public void onAnalysisBrightness(boolean isDark) {
        BarCodeUtil.d("isDark : " + isDark);

        darkList.addFirst(isDark);
        if (darkList.size() > DARK_LIST_SIZE) {
            darkList.removeLast();
        }

        int show = 0;
        for (Boolean dark : darkList) {
            if (dark) {
                show++;
            }
        }

        if (this.isDark) {
            if (show <= 2) {
                this.isDark = false;
                mScanBoxView.setDark(false);
            }
        } else {
            if (show >= DARK_LIST_SIZE) {
                this.isDark = true;
                mScanBoxView.setDark(true);
            }
        }
    }

    public void resetZoom() {
        setZoomValue(0);
    }

    @Override
    public void onFlashLightClick() {
        mCameraSurface.toggleFlashLight(isDark);
    }

}
