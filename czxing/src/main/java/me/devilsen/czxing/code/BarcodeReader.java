package me.devilsen.czxing.code;

import android.graphics.Bitmap;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.BitmapUtil;

public class BarcodeReader {

    private long _nativePtr;

    private static final class InstanceHolder {
        static final BarcodeReader instance = new BarcodeReader();
    }

    public static BarcodeReader getInstance() {
        return InstanceHolder.instance;
    }

    private BarcodeReader() {
        BarcodeFormat[] formats = new BarcodeFormat[]{BarcodeFormat.QR_CODE};
        _nativePtr = NativeSdk.getInstance().createInstance(getNativeFormats(formats));
    }

    public void setDetectModel(String detectorPrototxtPath, String detectorCaffeModelPath, String superResolutionPrototxtPath, String superResolutionCaffeModelPath) {
        NativeSdk.getInstance().setDetectModel(_nativePtr, detectorPrototxtPath, detectorCaffeModelPath,
                superResolutionPrototxtPath, superResolutionCaffeModelPath);
    }

    public void setBarcodeFormat(BarcodeFormat... formats) {
        NativeSdk.getInstance().setFormat(_nativePtr, getNativeFormats(formats));
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
    public List<CodeResult> read(Bitmap bitmap) {
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

        CodeResult[] result = NativeSdk.getInstance().readBitmap(_nativePtr, newBitmap);
        bitmap.recycle();
        newBitmap.recycle();
        return processResult(result);
    }

    @NonNull
    @CheckResult
    public List<CodeResult> readDetect(Bitmap bitmap) {
        if (bitmap == null) {
            BarCodeUtil.e("bitmap is null");
            return new ArrayList<>(0);
        }
        int imgWidth = bitmap.getWidth();
        int imgHeight = bitmap.getHeight();
        BarCodeUtil.d("bitmap width = " + imgWidth + " height = " + imgHeight);

        CodeResult[] result = NativeSdk.getInstance().readBitmap(_nativePtr, bitmap);
        bitmap.recycle();
        return processResult(result);
    }

    @NonNull
    @CheckResult
    public List<CodeResult> read(byte[] data, int cropLeft, int cropTop, int cropWidth, int cropHeight, int rowWidth, int rowHeight) {
        CodeResult[] result = NativeSdk.getInstance().readByte(_nativePtr, data, cropLeft, cropTop, cropWidth, cropHeight, rowWidth, rowHeight);
        return processResult(result);
    }

    @NonNull
    @CheckResult
    private List<CodeResult> processResult(CodeResult[] result) {
        if (result != null) {
            return Arrays.asList(result);
        }
        return new ArrayList<>(0);
    }

    private BarcodeFormat getFormat(Object result) {
        int[] formatInts = (int[]) result;
        return BarcodeFormat.valueOf(formatInts[0]);
    }

    private int[] getPoints(Object pointArray) {
        if (pointArray == null) return new int[]{};
        return (int[]) pointArray;
    }

    @Deprecated
    public void enableCVDetect(boolean enable) {
//        if (enable) {
//            NativeSdk.getInstance().openCVDetectValue(_nativePtr, 10);
//        } else {
//            NativeSdk.getInstance().openCVDetectValue(_nativePtr, 0);
//        }
    }

    @Deprecated
    public void prepareRead() {
//        NativeSdk.getInstance().prepareRead(_nativePtr);
    }

    @Deprecated
    public void stopRead() {
//        NativeSdk.getInstance().stopRead(_nativePtr);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (_nativePtr != 0) {
                NativeSdk.getInstance().destroyInstance(_nativePtr);
                _nativePtr = 0;
            }
        } finally {
            super.finalize();
        }
    }

    public void setReadCodeListener(ReadCodeListener readCodeListener) {
        NativeSdk.getInstance().setReadCodeListener(readCodeListener);
    }

    public interface ReadCodeListener {
        void onReadCodeResult(List<CodeResult> resultList);

        void onFocus();

        void onAnalysisBrightness(boolean isDark);
    }
}
