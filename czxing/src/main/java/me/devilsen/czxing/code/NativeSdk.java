package me.devilsen.czxing.code;

import android.graphics.Bitmap;

/**
 * desc: Jni connector
 * date: 2019/08/17 0017
 *
 * @author : dongsen
 */
class NativeSdk {

    private NativeSdk() {
    }

    public static NativeSdk getInstance() {
        return Holder.instance;
    }

    private static class Holder {
        private static final NativeSdk instance = new NativeSdk();
    }

    private BarcodeReader.ReadCodeListener readCodeListener;

    void setReadCodeListener(BarcodeReader.ReadCodeListener readCodeListener) {
        this.readCodeListener = readCodeListener;
    }

    /**
     * Native Callback for scan result
     *
     * @param content     识别出的文字
     * @param formatIndex 格式
     * @param points      定位点的位置
     * @param scanType    扫码类型，用于还原定位点的位置
     */
    public void onDecodeCallback(String content, int formatIndex, float[] points, int scanType) {
        if (readCodeListener != null) {
            readCodeListener.onReadCodeResult(new CodeResult(content, formatIndex, points, scanType));
        }
    }

    /**
     * Native Callback for focus
     */
    public void onFocusCallback() {
        if (readCodeListener != null) {
            readCodeListener.onFocus();
        }
    }

    /**
     * Native callback for brightness
     *
     * @param isDark true : bright too low
     */
    public void onBrightnessCallback(boolean isDark) {
        if (readCodeListener != null) {
            readCodeListener.onAnalysisBrightness(isDark);
        }
    }

    // read
    native long createInstance(int[] formats);

    native void destroyInstance(long objPtr);

    native void setFormat(long objPtr, int[] formats);

    native void setDetectModel(long objPtr, String detectorPrototxtPath, String detectorCaffeModelPath, String superResolutionPrototxtPath, String superResolutionCaffeModelPath);

    native int readByte(long objPtr, byte[] bytes, int left, int top, int width, int height, int rowWidth, int rowHeight, Object[] result);

    native int readBitmap(long objPtr, Bitmap bitmap, Object[] result);

    // write
    native int writeCode(String content, int width, int height, int color, String format, Object[] result);

    static {
        System.loadLibrary("czxing");
    }

}
