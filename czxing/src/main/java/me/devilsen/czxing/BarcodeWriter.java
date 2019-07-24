package me.devilsen.czxing;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * desc : 生成二维码处理
 * date : 2019-07-22 15:35
 *
 * @author : dongSen
 */
public class BarcodeWriter {

    /**
     * 生成二维码
     *
     * @param text   要生成的文本
     * @param width  bitmap 宽
     * @param height bitmap 高
     * @return bitmap二维码
     */
    public Bitmap write(String text, int width, int height) {
        return write(text, width, height, Color.BLACK);
    }

    /**
     * 生成二维码
     *
     * @param text   要生成的文本
     * @param width  bitmap 宽
     * @param height bitmap 高
     * @param color  要生成的二维码颜色
     * @return bitmap二维码
     */
    public Bitmap write(String text, int width, int height, int color) {
        Object[] result = new Object[1];
        int resultCode = writeBarcode(text, width, height, color, result);
        if (resultCode > -1) {
            int[] pixels = (int[]) result[0];
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        }
        return null;
    }

    public static native int writeBarcode(String content, int width, int height, int color, Object[] result);

    static {
        System.loadLibrary("zxing-lib");
    }

}
