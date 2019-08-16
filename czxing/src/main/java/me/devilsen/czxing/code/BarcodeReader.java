package me.devilsen.czxing.code;

import android.graphics.Bitmap;
import android.util.Log;

import me.devilsen.czxing.util.BarCodeUtil;

public class BarcodeReader {

    private long _nativePtr;

    public BarcodeReader(BarcodeFormat... formats) {
        int[] nativeFormats = new int[formats.length];
        for (int i = 0; i < formats.length; ++i) {
            nativeFormats[i] = formats[i].ordinal();
        }
        _nativePtr = NativeSdk.createInstance(nativeFormats);
    }

    public CodeResult read(Bitmap bitmap) {
        int imgWidth = bitmap.getWidth();
        int imgHeight = bitmap.getHeight();
        Object[] result = new Object[2];
        int resultFormat = NativeSdk.readBarcode(_nativePtr, bitmap, 0, 0, imgWidth, imgHeight, result);
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
            Object[] result = new Object[3];
            int resultFormat = NativeSdk.readBarcodeByte(_nativePtr, data, cropLeft, cropTop, cropWidth, cropHeight, rowWidth, rowHeight, result);
            if (resultFormat > 0) {
                CodeResult decodeResult = new CodeResult(BarcodeFormat.values()[resultFormat], (String) result[0]);
                if (result[1] != null) {
                    decodeResult.setPoint((float[]) result[1]);
                } else if (result[2] != null) {
                    decodeResult.setPoint((int[]) result[2]);
                }
                return decodeResult;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public boolean analysisBrightness(byte[] data, int imageWidth, int imageHeight) {
        return NativeSdk.analysisBrightnessNative(data, imageWidth, imageHeight);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (_nativePtr != 0) {
                NativeSdk.destroyInstance(_nativePtr);
                _nativePtr = 0;
            }
        } finally {
            super.finalize();
        }
    }
}
