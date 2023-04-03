package me.devilsen.czxing.code;

import android.graphics.Bitmap;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.devilsen.czxing.thread.ExecutorUtil;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.BitmapUtil;

public class BarcodeDecoder {

    private final DecodeEngine mEngine;

    public BarcodeDecoder() {
        this(BarcodeFormat.QR_CODE);
    }

    public BarcodeDecoder(BarcodeFormat... formats) {
        mEngine = new DecodeEngine(getNativeFormats(formats));
    }

    public void destroy() {
        mEngine.destroyInstance();
    }

    public void setDetectModel(String detectorPrototxtPath, String detectorCaffeModelPath, String superResolutionPrototxtPath, String superResolutionCaffeModelPath) {
        mEngine.setDetectModel(detectorPrototxtPath, detectorCaffeModelPath,
                superResolutionPrototxtPath, superResolutionCaffeModelPath);
    }

    public void setBarcodeFormat(BarcodeFormat... formats) {
        if (formats == null || formats.length == 0) {
            return;
        }
        mEngine.setFormat(getNativeFormats(formats));
    }

    private int[] getNativeFormats(BarcodeFormat... formats) {
        int[] nativeFormats = new int[formats.length];
        for (int i = 0; i < formats.length; ++i) {
            nativeFormats[i] = formats[i].getValue();
        }
        return nativeFormats;
    }

    @NonNull
    @CheckResult
    public List<CodeResult> decodeBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            BarCodeUtil.e("bitmap is null");
            return new ArrayList<>(0);
        }
        // 尝试放大，识别更复杂的二维码
        if ((bitmap.getHeight() < 2000 || bitmap.getWidth() < 1100) && bitmap.getHeight() < 3000) {
            BarCodeUtil.d("zoom bitmap");
            bitmap = BitmapUtil.zoomBitmap(bitmap);
        }
        // 避免某些情况无法获取图片格式的问题
        Bitmap newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        int imgWidth = newBitmap.getWidth();
        int imgHeight = newBitmap.getHeight();
        BarCodeUtil.d("bitmap width = " + imgWidth + " height = " + imgHeight);

        CodeResult[] result = mEngine.decodeBitmap(newBitmap);
        newBitmap.recycle();
        return asList(result);
    }

    @NonNull
    @CheckResult
    public List<CodeResult> decodeYUV(byte[] data, int cropLeft, int cropTop, int cropWidth, int cropHeight, int rowWidth, int rowHeight) {
        CodeResult[] result = mEngine.decodeByte(data, cropLeft, cropTop, cropWidth, cropHeight, rowWidth, rowHeight);
        return asList(result);
    }

    @CheckResult
    public double detectBrightness(byte[] data, int rowWidth, int rowHeight) {
        return mEngine.detectBrightness(data, rowWidth, rowHeight);
    }

    public void decodeBitmapASync(final Bitmap bitmap, final OnDetectCodeListener listener) {
        ExecutorUtil.getCalculateExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onReadCodeResult(decodeBitmap(bitmap));
                }
            }
        });
    }

    public void decodeYUVASync(final byte[] data, final int cropLeft, final int cropTop, final int cropWidth, final int cropHeight, final int rowWidth, final int rowHeight, final OnDetectCodeListener listener) {
        decodeYUVASync(data, cropLeft, cropTop, cropWidth, cropHeight, rowWidth, rowHeight, listener, null);
    }

    public void decodeYUVASync(final byte[] data, final int cropLeft, final int cropTop, final int cropWidth, final int cropHeight, final int rowWidth, final int rowHeight, final OnDetectCodeListener listener, final OnFocusListener focusListener) {
        ExecutorUtil.getCalculateExecutor().execute(new Runnable() {
            @Override
            public void run() {
                List<CodeResult> codeResults = decodeYUV(data, cropLeft, cropTop, cropWidth, cropHeight, rowWidth, rowHeight);
                if (listener != null) {
                    listener.onReadCodeResult(codeResults);
                }
                if (codeResults.isEmpty() && focusListener != null) {
                    focusListener.onFocus();
                }
            }
        });
    }

    public void detectBrightnessASync(final byte[] data, final int rowWidth, final int rowHeight, final OnDetectBrightnessListener listener) {
        ExecutorUtil.getSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onAnalysisBrightness(detectBrightness(data, rowWidth, rowHeight));
                }
            }
        });
    }

    @NonNull
    @CheckResult
    private List<CodeResult> asList(CodeResult[] result) {
        if (result != null) {
            return Arrays.asList(result);
        }
        return new ArrayList<>(0);
    }

    public interface OnDetectCodeListener {
        void onReadCodeResult(List<CodeResult> resultList);
    }

    public interface OnDetectBrightnessListener {
        void onAnalysisBrightness(double brightness);
    }

    public interface OnFocusListener {
        void onFocus();
    }

}
