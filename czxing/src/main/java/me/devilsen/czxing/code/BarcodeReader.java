package me.devilsen.czxing.code;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.SaveImageUtil;

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
        BarcodeFormat[] formats = new BarcodeFormat[]{BarcodeFormat.QR_CODE,
                BarcodeFormat.CODABAR,
                BarcodeFormat.CODE_128,
                BarcodeFormat.EAN_13,
                BarcodeFormat.UPC_A};
        _nativePtr = NativeSdk.getInstance().createInstance(getNativeFormats(formats));
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

    public CodeResult read(Bitmap bitmap) {
        if (bitmap == null) {
            BarCodeUtil.e("bitmap is null");
            return null;
        }
        // 避免某些情况无法获取图片格式的问题
        Bitmap newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        int imgWidth = newBitmap.getWidth();
        int imgHeight = newBitmap.getHeight();
        Object[] result = new Object[2];
        int resultFormat = NativeSdk.getInstance().readBarcode(_nativePtr, newBitmap, 0, 0, imgWidth, imgHeight, result);
        if (resultFormat >= 0) {
            CodeResult decodeResult = new CodeResult(BarcodeFormat.values()[resultFormat], (String) result[0]);
            if (result[1] != null) {
                decodeResult.setPoint((float[]) result[1]);
            }
            return decodeResult;
        }
        return null;
    }

    public CodeResult read(byte[] data, int cropLeft, int cropTop, int cropWidth, int cropHeight, int rowWidth, int rowHeight) {
        try {
            NativeSdk.getInstance().readBarcodeByte(_nativePtr, data, cropLeft, cropTop, cropWidth, cropHeight, rowWidth, rowHeight);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void prepareRead() {
        NativeSdk.getInstance().prepareRead(_nativePtr);
    }

    public void stopRead() {
        NativeSdk.getInstance().stopRead(_nativePtr);
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
