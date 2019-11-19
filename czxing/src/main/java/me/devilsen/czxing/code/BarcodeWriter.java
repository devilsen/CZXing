package me.devilsen.czxing.code;

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
     * @param size  图片边长
     * @param color 要生成的二维码颜色
     * @param logo  放在中间的logo
     * @return bitmap二维码
     */
    public Bitmap write(String text, int size, int color, Bitmap logo) {
        return write(text, size, size, color, BarcodeFormat.QR_CODE, logo);
    }

    /**
     * 生成一维码
     *
     * @param text   要生成的文字（不支持中文）
     * @param width  图片宽
     * @param height 图片高
     * @return 一维码bitmap
     */
    public Bitmap writeBarCode(String text, int width, int height) {
        return writeBarCode(text, width, height, Color.BLACK);
    }

    /**
     * 生成一维码
     *
     * @param text   要生成的文字（不支持中文）
     * @param width  图片宽
     * @param height 图片高
     * @param format 一维码格式
     * @return 一维码bitmap
     */
    public Bitmap writeBarCode(String text, int width, int height, BarcodeFormat format) {
        return write(text, width, height, Color.BLACK, format, null);
    }

    /**
     * 生成一维码
     *
     * @param text   要生成的文字（不支持中文）
     * @param width  图片宽
     * @param height 图片高
     * @param color  一维码颜色
     * @return 一维码bitmap
     */
    public Bitmap writeBarCode(String text, int width, int height, int color) {
        return write(text, width, height, color, BarcodeFormat.CODE_128, null);
    }

    /**
     * 生成图片
     *
     * @param text   要生成的文本
     * @param width  图片宽
     * @param height 图片高
     * @param color  要生成的二维码颜色
     * @param format 要生成的条码格式
     * @param logo   放在中间的logo
     * @return bitmap二维码
     */
    private Bitmap write(String text, int width, int height, int color, BarcodeFormat format, Bitmap logo) {
        Object[] result = new Object[1];
        int resultCode = NativeSdk.getInstance().writeCode(text, width, height, color, format.name(), result);
        Bitmap bitmap = null;
        if (resultCode > -1) {
            int[] pixels = (int[]) result[0];
            bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
            // 添加logo
            if (logo != null) {
                bitmap = BitmapUtil.addLogoInQRCode(bitmap, logo);
            }
        }
        return bitmap;
    }

}
