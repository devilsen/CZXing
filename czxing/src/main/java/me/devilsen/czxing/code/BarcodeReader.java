package me.devilsen.czxing.code;

import android.graphics.Bitmap;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;

import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.BitmapUtil;

public class BarcodeReader {

    private long _nativePtr;
    private static BarcodeReader instance;

    public static BarcodeReader getInstance() {
        if (instance == null) {
            synchronized (BarcodeReader.class) {
                if (instance == null) {
                    instance = new BarcodeReader();
                }
            }
        }
        return instance;
    }

    private BarcodeReader() {
        BarcodeFormat[] formats = new BarcodeFormat[]{BarcodeFormat.QR_CODE};
        _nativePtr = NativeSdk.getInstance().createInstance(getNativeFormats(formats));
    }

    public void setDetectModel(String detectorPrototxtPath, String detectorCaffeModelPath, String superResolutionPrototxtPath, String superResolutionCaffeModelPath) {
        NativeSdk.getInstance().setDetectModel(_nativePtr,
                detectorPrototxtPath, detectorCaffeModelPath,
                superResolutionPrototxtPath, superResolutionCaffeModelPath);
    }

    public void setBarcodeFormat(BarcodeFormat... formats) {
        NativeSdk.getInstance().setFormat(_nativePtr, getNativeFormats(formats));
    }

    private int[] getNativeFormats(BarcodeFormat... formats) {
        int[] nativeFormats = new int[formats.length];
        for (int i = 0; i < formats.length; ++i) {
            nativeFormats[i] = formats[i].ordinal();
        }
        return nativeFormats;
    }

    @Nullable
    @CheckResult
    public CodeResult read(Bitmap bitmap) {
        if (bitmap == null) {
            BarCodeUtil.e("bitmap is null");
            return null;
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

        Object[] result = new Object[3];
        int resultFormat = NativeSdk.getInstance().readBitmap(_nativePtr, newBitmap, result);
        bitmap.recycle();
        newBitmap.recycle();
        return processResult(resultFormat, result);
    }

    @Nullable
    @CheckResult
    public CodeResult readDetect(Bitmap bitmap) {
        if (bitmap == null) {
            BarCodeUtil.e("bitmap is null");
            return null;
        }
        int imgWidth = bitmap.getWidth();
        int imgHeight = bitmap.getHeight();
        BarCodeUtil.d("bitmap width = " + imgWidth + " height = " + imgHeight);

        Object[] result = new Object[3];
        int resultFormat = NativeSdk.getInstance().readBitmap(_nativePtr, bitmap, result);
        bitmap.recycle();
        return processResult(resultFormat, result);
    }

    @Nullable
    @CheckResult
    public CodeResult read(byte[] data, int cropLeft, int cropTop, int cropWidth, int cropHeight, int rowWidth, int rowHeight) {
        Object[] result = new Object[3];
        int resultFormat = NativeSdk.getInstance().readByte(_nativePtr, data, cropLeft, cropTop, cropWidth, cropHeight, rowWidth, rowHeight, result);
        return processResult(resultFormat, result);
    }

    @Nullable
    @CheckResult
    private CodeResult processResult(int resultFormat, Object[] result) {
        if (resultFormat >= 0) {
            int[] formatInts = (int[]) result[0];
            BarcodeFormat format = BarcodeFormat.valueOf(formatInts[0]);
            CodeResult decodeResult = new CodeResult(format, (String) result[1]);
            if (result[2] != null) {
                decodeResult.setPoint((int[]) result[2]);
            }
            return decodeResult;
        }
        return null;
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
        void onReadCodeResult(CodeResult result);

        void onFocus();

        void onAnalysisBrightness(boolean isDark);
    }
}
