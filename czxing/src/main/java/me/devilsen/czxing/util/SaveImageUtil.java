package me.devilsen.czxing.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * desc :
 * date : 2019-07-01 22:31
 *
 * @author : dongSen
 */
public class SaveImageUtil {

    public static void saveData(byte[] data, int left, int top, int width, int height, int rowWidth) {
        if (System.currentTimeMillis() - time < 5000) {
            return;
        }
        time = System.currentTimeMillis();
        Log.e("save >>> ", "left = " + left + " top= " + top +
                " width=" + width + " height= " + height + " row=" + rowWidth);

        int[] rgba = applyGrayScale(data, left, top, width, height, rowWidth);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0, width, 0, 0, width, height);
        saveImage(bmp);

        bmp = getBinaryzationBitmap(bmp);
        saveImage(bmp);

        bmp.recycle();
    }

    private static int[] applyGrayScale(byte[] data, int left, int top, int width, int height, int rowWidth) {
        int p;
        int[] pixels = new int[width * height];
        int desIndex = 0;
        int srcIndex = top * rowWidth;
        int margin = rowWidth - left - width;
        for (int i = top; i < height + top; ++i) {
            srcIndex += left;
            for (int j = left; j < left + width; ++j, ++desIndex, ++srcIndex) {
                p = data[srcIndex] & 0xFF;
                pixels[desIndex] = 0xff000000 | p << 16 | p << 8 | p;
            }
            srcIndex += margin;
        }
        return pixels;
    }


    /**
     * 对图片进行二值化处理
     *
     * @param bm 原始图片
     * @return 二值化处理后的图片
     */
    public static Bitmap getBinaryzationBitmap(Bitmap bm) {
        Bitmap bitmap = null;
        // 获取图片的宽和高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 创建二值化图像
        bitmap = bm.copy(Bitmap.Config.ARGB_8888, true);
        // 遍历原始图像像素,并进行二值化处理
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // 得到当前的像素值
                int pixel = bitmap.getPixel(i, j);
                // 得到Alpha通道的值
                int alpha = pixel & 0xFF000000;
                // 得到Red的值
                int red = (pixel & 0x00FF0000) >> 16;
                // 得到Green的值
                int green = (pixel & 0x0000FF00) >> 8;
                // 得到Blue的值
                int blue = pixel & 0x000000FF;
                // 通过加权平均算法,计算出最佳像素值
                int gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
                // 对图像设置黑白图
                if (gray <= 150) {
                    gray = 0;
                } else {
                    gray = 255;
                }
                // 得到新的像素值
                int newPiexl = alpha | (gray << 16) | (gray << 8) | gray;
                // 赋予新图像的像素
                bitmap.setPixel(i, j, newPiexl);
            }
        }
        return bitmap;
    }

    private static long time;

    private static void saveImage(Bitmap bitmap) {


        String thumbPath = System.currentTimeMillis() + ".jpg";
        String fold = Environment.getExternalStorageDirectory().getAbsolutePath() + "/scan/";
        File file = new File(fold, thumbPath);

        FileOutputStream out = null;

        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }

            out = new FileOutputStream(file);

            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)) {
                out.flush();
                out.close();
                Log.e("save >>> ", "save image success");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
