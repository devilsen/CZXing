package me.devilsen.czxing;

import android.graphics.Bitmap;

public class BarcodeReader {

    public static class Result {
        public BarcodeFormat getFormat() {
            return format;
        }

        public String getText() {
            return text;
        }

        Result(BarcodeFormat format, String text) {
            this.format = format;
            this.text = text;
        }

        private BarcodeFormat format;
        private String text;
    }

    public BarcodeReader(BarcodeFormat... formats) {
        int[] nativeFormats = new int[formats.length];
        for (int i = 0; i < formats.length; ++i) {
            nativeFormats[i] = formats[i].ordinal();
        }
        _nativePtr = createInstance(nativeFormats);
    }

    public Result read(Bitmap bitmap, int cropWidth, int cropHeight) {
        int imgWidth = bitmap.getWidth();
        int imgHeight = bitmap.getHeight();
        cropWidth = cropWidth <= 0 ? imgWidth : Math.min(imgWidth, cropWidth);
        cropHeight = cropHeight <= 0 ? imgHeight : Math.min(imgHeight, cropHeight);
        int cropLeft = (imgWidth - cropWidth) / 2;
        int cropTop = (imgHeight - cropHeight) / 2;
        Object[] result = new Object[1];
        int resultFormat = readBarcode(_nativePtr, bitmap, cropLeft, cropTop, cropWidth, cropHeight, result);
        if (resultFormat >= 0) {
            return new Result(BarcodeFormat.values()[resultFormat], (String) result[0]);
        }
        return null;
    }

    public Result read(byte[] data, int cropWidth, int cropHeight, int imgWidth, int imgHeight) {
        cropWidth = cropWidth <= 0 ? imgWidth : Math.min(imgWidth, cropWidth);
        cropHeight = cropHeight <= 0 ? imgHeight : Math.min(imgHeight, cropHeight);
        int cropLeft = (imgWidth - cropWidth) / 2;
        int cropTop = (imgHeight - cropHeight) / 2;
        Object[] result = new Object[1];
        int resultFormat = readBarcodeByte(_nativePtr, data, cropLeft, cropTop, cropWidth, cropHeight, result);
        if (resultFormat >= 0) {
            return new Result(BarcodeFormat.values()[resultFormat], (String) result[0]);
        }
        return null;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (_nativePtr != 0) {
                destroyInstance(_nativePtr);
                _nativePtr = 0;
            }
        } finally {
            super.finalize();
        }
    }


    private long _nativePtr;

    private static native long createInstance(int[] formats);

    private static native void destroyInstance(long objPtr);

    private static native int readBarcode(long objPtr, Bitmap bitmap, int left, int top, int width, int height, Object[] result);

    private static native int readBarcodeByte(long objPtr, byte[] bytes, int left, int top, int width, int height, Object[] result);

    static {
        System.loadLibrary("zxing-lib");
    }

}
