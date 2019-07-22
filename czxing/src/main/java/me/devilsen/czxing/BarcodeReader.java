package me.devilsen.czxing;

import android.graphics.Bitmap;

import me.devilsen.czxing.util.BarCodeUtil;

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
            logPoint();
        }

        public void setPoint(int[] lists) {
            points = new float[lists.length];
            for (int i = 0; i < lists.length; i++) {
                points[i] = lists[i];
            }
            logPoint();
        }

        private void logPoint() {
            StringBuilder stringBuilder = new StringBuilder("location points ");
            int i = 0;
            for (float list : points) {
                i++;
                stringBuilder.append(list).append("  ");
                if (i % 2 == 0) {
                    stringBuilder.append("\n");
                }
            }
            BarCodeUtil.d(stringBuilder.toString());
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
            Object[] result = new Object[3];
            int resultFormat = readBarcodeByte(_nativePtr, data, cropLeft, cropTop, cropWidth, cropHeight, rowWidth, result);
            if (resultFormat > 0) {
                Result decodeResult = new Result(BarcodeFormat.values()[resultFormat], (String) result[0]);
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

    public Bitmap write(String text, int width, int height) {
        Object[] result = new Object[1];
        int resultCode = writeBarcode(text, width, height, result);
        if (resultCode > -1) {
            int[] pixels = (int[]) result[0];
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
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

    public static native boolean analysisBrightnessNative(byte[] bytes, int width, int height);

    public static native int writeBarcode(String content, int width, int height, Object[] result);

    static {
        System.loadLibrary("zxing-lib");
    }

}
