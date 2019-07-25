package me.devilsen.czxing;

import android.graphics.Bitmap;
import android.graphics.Color;

import me.devilsen.czxing.util.BitmapUtil;

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
     * @param text 要生成的文本
     * @param size 边长
     * @return bitmap二维码
     */
    public Bitmap write(String text, int size) {
        return write(text, size, Color.BLACK);
    }

    /**
     * 生成二维码
     *
     * @param text  要生成的文本
     * @param size  边长
     * @param color 要生成的二维码颜色
     * @return bitmap二维码
     */
    public Bitmap write(String text, int size, int color) {
        return write(text, size, color, null);
    }

    /**
     * 生成带logo的二维码
     *
     * @param text  要生成的文本
     * @param size  边长
     * @param color 要生成的二维码颜色
     * @return bitmap二维码
     */
    public Bitmap write(String text, int size, int color, Bitmap logo) {
        Object[] result = new Object[1];
        int resultCode = writeBarcode(text, size, size, color, result);
        Bitmap bitmap = null;
        if (resultCode > -1) {
            int[] pixels = (int[]) result[0];
            bitmap = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888);
            // 添加logo
            if (logo != null) {
                bitmap = BitmapUtil.addLogoInQRCode(bitmap, logo);
            }
        }
        return bitmap;
    }

    public static native int writeBarcode(String content, int width, int height, int color, Object[] result);

    static {
        System.loadLibrary("zxing-lib");
    }

}
