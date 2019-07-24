package me.devilsen.czxing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
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
                bitmap = addLogoInQRCode(bitmap, logo);
            }
        }
        return bitmap;
    }

    /**
     * 添加logo到二维码图片上
     */
    private static Bitmap addLogoInQRCode(Bitmap src, Bitmap logo) {
        if (src == null || logo == null) {
            return src;
        }

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        int logoWidth = logo.getWidth();
        int logoHeight = logo.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(src, 0, 0, null);
            canvas.drawBitmap(logo, (srcWidth - logoWidth) >> 1, (srcHeight - logoHeight) >> 1, null);
            canvas.save();
            canvas.restore();
        } catch (Exception e) {
            e.printStackTrace();
            bitmap = src;
        }
        return bitmap;
    }

    public static native int writeBarcode(String content, int width, int height, int color, Object[] result);

    static {
        System.loadLibrary("zxing-lib");
    }

}
