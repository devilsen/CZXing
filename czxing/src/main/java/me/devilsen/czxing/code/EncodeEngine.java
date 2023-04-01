package me.devilsen.czxing.code;

/**
 * Created by dongSen on 2023/4/1
 */
public class EncodeEngine {

    public int writeCode(String content, int width, int height, int color, String format, Object[] result) {
        return nativeWriteCode(content, width, height, color, format, result);
    }

    // write
    private native int nativeWriteCode(String content, int width, int height, int color, String format, Object[] result);

    static {
        System.loadLibrary("czxing");
    }
}
