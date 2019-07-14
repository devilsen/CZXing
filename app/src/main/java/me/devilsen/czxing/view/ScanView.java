package me.devilsen.czxing.view;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;

import me.devilsen.czxing.BarCodeUtil;
import me.devilsen.czxing.BarcodeFormat;
import me.devilsen.czxing.BarcodeReader;
import me.devilsen.czxing.thread.Callback;
import me.devilsen.czxing.thread.Dispatcher;

/**
 * @author : dongSen
 * date : 2019-06-29 16:18
 * desc :
 */
public class ScanView extends BarCoderView implements Callback, ScanBoxView.ScanBoxClickListener {

    private Dispatcher mDispatcher;
    private boolean isDark;
    private BarcodeReader reader;

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
        reader = new BarcodeReader(BarcodeFormat.QR_CODE);
    }

    public void onResume(){
        String path = new File(Environment.getExternalStorageDirectory(), "qrcode_cascade.xml").getAbsolutePath();

        reader.initOpenCV(path);
    }

    @Override
    public void onPreviewFrame(byte[] data, int left, int top, int width, int height, int rowWidth) {
//        SaveImageUtil.saveData(data, left, top, width, height, rowWidth);
//        mDispatcher.newRunnable(data, left, top, width, height, rowWidth, this).enqueue();
        reader.postData(data, rowWidth, top + height);

    }

    @Override
    public void stopScan() {
        super.stopScan();
        mDispatcher.cancelAll();
    }

    @Override
    public void onDecodeComplete(BarcodeReader.Result result) {
        if (result == null) {
            return;
        }
        if (!TextUtils.isEmpty(result.getText())) {
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

    }

    private void tryZoom(BarcodeReader.Result result) {
        int len = 0;
        float[] points = result.getPoints();
        if (points.length > 3) {
            float point1X = points[0];
            float point1Y = points[1];
            float point2X = points[2];
            float point2Y = points[3];
            float xLen = Math.abs(point1X - point2X);
            float yLen = Math.abs(point1Y - point2Y);
            len = (int) Math.sqrt(xLen * xLen + yLen * yLen);
        }

        if (points.length > 5) {
            float point2X = points[2];
            float point2Y = points[3];
            float point3X = points[4];
            float point3Y = points[5];
            float xLen = Math.abs(point2X - point3X);
            float yLen = Math.abs(point2Y - point3Y);
            int len2 = (int) Math.sqrt(xLen * xLen + yLen * yLen);
            if (len2 < len) {
                len = len2;
            }
        }

        Log.e("len", len + "");

        if (len > 0) {
            handleAutoZoom(len);
        }
    }

}
