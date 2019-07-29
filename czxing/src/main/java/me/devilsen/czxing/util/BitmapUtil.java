package me.devilsen.czxing.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

/**
 * desc : bitmap 工具
 * date : 2019-07-25 20:43
 *
 * @author : dongSen
 */
public class BitmapUtil {

    /**
     * 添加logo到二维码图片上
     */
    public static Bitmap addLogoInQRCode(Bitmap original, Bitmap logo) {
        if (original == null || logo == null) {
            return original;
        }

        int srcWidth = original.getWidth();
        int srcHeight = original.getHeight();
        int logoWidth = logo.getWidth();
        int logoHeight = logo.getHeight();

        if (logoWidth > srcWidth) {
            logoWidth = srcWidth / 4;
            logoHeight = srcHeight / 4;
            logo = getResizedBitmap(logo, logoWidth, logoHeight);
        }

        Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(original, 0, 0, null);
            canvas.drawBitmap(logo, (srcWidth - logoWidth) >> 1, (srcHeight - logoHeight) >> 1, null);
            canvas.save();
            canvas.restore();
        } catch (Exception e) {
            e.printStackTrace();
            bitmap = original;
        }
        return bitmap;
    }

    private static Bitmap getResizedBitmap(Bitmap original, int newWidth, int newHeight) {
        int width = original.getWidth();
        int height = original.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(original, 0, 0, width, height, matrix, false);
        original.recycle();
        return resizedBitmap;
    }

    public static Bitmap rotateBitmap(Bitmap original, float degrees) {
        int width = original.getWidth();
        int height = original.getHeight();

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);

        Bitmap rotatedBitmap = Bitmap.createBitmap(original, 0, 0, width, height, matrix, true);
        original.recycle();
        return rotatedBitmap;
    }
}
