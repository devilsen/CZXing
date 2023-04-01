package me.devilsen.czxing.code;

import android.graphics.Bitmap;

/**
 * desc: Jni connector
 * date: 2019/08/17 0017
 *
 * @author : dongsen
 */
public class DecodeEngine {

    private long mNativePtr;

    public DecodeEngine(int[] formats) {
        mNativePtr = nativeCreateInstance(formats);
    }

    public void destroyInstance() {
        if (mNativePtr != 0) {
            nativeDestroyInstance(mNativePtr);
            mNativePtr = 0;
        }
    }

    public void setFormat(int[] formats) {
        nativeSetFormat(mNativePtr, formats);
    }

    public void setDetectModel(String detectorPrototxtPath, String detectorCaffeModelPath, String superResolutionPrototxtPath, String superResolutionCaffeModelPath) {
        nativeSetDetectModel(mNativePtr, detectorPrototxtPath, detectorCaffeModelPath, superResolutionPrototxtPath, superResolutionCaffeModelPath);
    }

    public CodeResult[] decodeByte(byte[] bytes, int left, int top, int width, int height, int rowWidth, int rowHeight) {
        return nativeDecodeByte(mNativePtr, bytes, left, top, width, height, rowWidth, rowHeight);
    }

    public CodeResult[] decodeBitmap(Bitmap bitmap) {
        return nativeDecodeBitmap(mNativePtr, bitmap);
    }

    public double detectBrightness(byte[] bytes, int rowWidth, int rowHeight) {
        return nativeDetectBrightness(mNativePtr, bytes, rowWidth, rowHeight);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            destroyInstance();
        } finally {
            super.finalize();
        }
    }

    // read
    private native long nativeCreateInstance(int[] formats);
    private native void nativeDestroyInstance(long objPtr);
    private native void nativeSetFormat(long objPtr, int[] formats);
    private native void nativeSetDetectModel(long objPtr, String detectorPrototxtPath, String detectorCaffeModelPath, String superResolutionPrototxtPath, String superResolutionCaffeModelPath);
    private native CodeResult[] nativeDecodeByte(long objPtr, byte[] bytes, int left, int top, int width, int height, int rowWidth, int rowHeight);
    private native CodeResult[] nativeDecodeBitmap(long objPtr, Bitmap bitmap);
    private native double nativeDetectBrightness(long objPtr, byte[] bytes, int rowWidth, int rowHeight);

    static {
        System.loadLibrary("czxing");
    }
}
