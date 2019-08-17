package me.devilsen.czxing.code;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * desc: Jni connector
 * date: 2019/08/17 0017
 *
 * @author : dongsen
 */
public class NativeSdk {

    static {
        System.loadLibrary("czxing");
    }

    // read
    static native long createInstance(int[] formats);

    static native void destroyInstance(long objPtr);

    static native int readBarcode(long objPtr, Bitmap bitmap, int left, int top, int width, int height, Object[] result);

    static native int readBarcodeByte(long objPtr, byte[] bytes, int left, int top, int width, int height, int rowWidth, int rowHeight, Object[] result);

    static native boolean analysisBrightnessNative(byte[] bytes, int width, int height);

    // write
    static native int writeCode(String content, int width, int height, int color, String format, Object[] result);

    // test
    public static native boolean callbackTest();


    /**
     * Native Callback
     *
     * @param content     识别出的文字
     * @param formatIndex 格式
     * @param points      定位点的位置
     */
    public void onDecodeCallback(String content, int formatIndex, float[] points) {
//        if (decodeListener != null) {
//            decodeListener.onDecodeResult(text);
//        }
        Log.e("result", "content : " + content + " formatIndex: " + formatIndex);
        if (points.length > 0) {
            Log.e("result", "points :" + points[0] + " " + points[1]);
        }
    }

    public void onTest(int i) {
//        if (decodeListener != null) {
//            decodeListener.onDecodeResult(text);
//        }
        Log.e("result", "bbbbbbbbbbbbbbbbbbbbbb  " + i);
    }

    public void callbackTestJava() {
        callbackTest();
    }

}
