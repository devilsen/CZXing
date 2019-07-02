package me.devilsen.czxing;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * desc :
 * date : 2019-06-27
 *
 * @author : dongSen
 */
public class Test {


    public Bitmap rawByteArray2RGBABitmap4(byte[] data, int width1, int height1) {
//        int[] rgba = yuv2rgb(data, width, height);
        Bitmap bitmap = BitmapFactory.decodeResource(MyApplication.getContext().getResources(), R.drawable.test1);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] rgba = new int[width * height];
        bitmap.getPixels(rgba, 0, width, 0, 0, width, height);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0, width, 0, 0, width, height);
        saveImage(bmp);

        return bmp;
    }

    private void saveImage(Bitmap bitmap) {
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

            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)) {
                out.flush();
                out.close();
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

    public static void main(String[] args) {
        int result = 5 % 4;
        System.out.println(result);
    }

}
