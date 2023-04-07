package me.devilsen.czxing.code;

/**
 * Created by dongSen on 2023/4/1
 */
public class EncodeEngine {

    /**
     * @param format {@link BarcodeFormat}
     * @param ecc    Error correction level, [0-8] Used for Aztec, PDF417, and QRCode only
     * @param margin Margin around barcode, Used for all formats,sets the minimum number of quiet zone pixels.
     *
     * @return 0:success -1:fail
     */
    public int writeCode(String content, int width, int height, int color, BarcodeFormat format, int ecc, int margin, Object[] result) {
        return nativeWriteCode(content, width, height, color, format.name(), ecc, margin, result);
    }

    // write
    private native int nativeWriteCode(String content, int width, int height, int color, String format, int ecc, int margin, Object[] result);

    static {
        System.loadLibrary("czxing");
    }
}
