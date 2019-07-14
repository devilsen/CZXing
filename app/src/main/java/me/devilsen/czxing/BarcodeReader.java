package me.devilsen.czxing;

import android.graphics.Bitmap;
import android.util.Log;

public class BarcodeReader {

    public static class Result {
        private BarcodeFormat format;
        private String text;
        private float[] points;

        public BarcodeFormat getFormat() {
            return format;
        }

        public String getText() {
            return text;
        }

        public float[] getPoints() {
            return points;
        }

        Result(BarcodeFormat format, String text) {
            this.format = format;
            this.text = text;
        }

        public void setPoint(float[] lists) {
            points = lists;
            StringBuilder stringBuilder = new StringBuilder();

            int i = 0;
            for (float list : lists) {
                i++;
                stringBuilder.append(list).append("  ");
                if (i % 2 == 0) {
                    stringBuilder.append("\n");
                }
            }
            Log.e("point ", stringBuilder.toString());
        }
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
        Object[] result = new Object[2];
        int resultFormat = readBarcode(_nativePtr, bitmap, cropLeft, cropTop, cropWidth, cropHeight, result);
        if (resultFormat >= 0) {
            Result decodeResult = new Result(BarcodeFormat.values()[resultFormat], (String) result[0]);
            if (result[1] != null) {
                decodeResult.setPoint((float[]) result[1]);
            }
            return decodeResult;
        }
        return null;
    }

    public Result read(byte[] data, int cropLeft, int cropTop, int cropWidth, int cropHeight, int rowWidth) {
        try {
            Object[] result = new Object[2];
            int resultFormat = readBarcodeByte(_nativePtr, data, cropLeft, cropTop, cropWidth, cropHeight, rowWidth, result);
            if (resultFormat > 0) {
                Result decodeResult = new Result(BarcodeFormat.values()[resultFormat], (String) result[0]);
                if (result[1] != null) {
                    decodeResult.setPoint((float[]) result[1]);
                }
                return decodeResult;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Result readFullImage(byte[] data, int imageWidth, int imageHeight) {
        Object[] result = new Object[2];
        int resultFormat = readBarcodeByteFullImage(_nativePtr, data, imageWidth, imageHeight, result);
        if (resultFormat >= 0) {
            Result decodeResult = new Result(BarcodeFormat.values()[resultFormat], (String) result[0]);
            if (result[1] != null) {
                decodeResult.setPoint((float[]) result[1]);
            }
            return decodeResult;
        }
        return null;
    }

    public boolean analysisBrightness(byte[] data, int imageWidth, int imageHeight) {
        return analysisBrightnessNative(data, imageWidth, imageHeight);
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

    private static native int readBarcodeByte(long objPtr, byte[] bytes, int left, int top, int width, int height, int rowWidth, Object[] result);

    private static native int readBarcodeByteFullImage(long objPtr, byte[] bytes, int width, int height, Object[] result);

    public static native boolean analysisBrightnessNative(byte[] bytes, int width, int height);

    public native void initOpenCV(String path);

    public native void postData(byte[] data, int width, int height);

    static {
        System.loadLibrary("zxing-lib");
    }

}
