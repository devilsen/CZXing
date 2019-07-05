package me.devilsen.czxing;

import android.graphics.Bitmap;
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
        Log.e("save >>> ", "left = " + left + " top= " + top +
                " width=" + width + " height= " + height + " row=" + rowWidth);

        int[] rgba = applyGrayScale(data, left, top, width, height, rowWidth);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0, width, 0, 0, width, height);
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

    private static long time;

    private static void saveImage(Bitmap bitmap) {
        if (System.currentTimeMillis() - time < 5000) {
            return;
        }
        time = System.currentTimeMillis();

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
