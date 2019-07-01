package me.devilsen.czxing.processor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * desc : 处理摄像头生成的数据
 * date : 2019-06-25
 *
 * @author : dongSen
 */
public class CameraDataProcessor extends Processor {

    private static final String TAG = "Scan >>>";

    private BarcodeProcessor barcodeProcessor;
    private ByteArrayOutputStream baos;

    public CameraDataProcessor() {
        barcodeProcessor = new BarcodeProcessor();
    }

    @Override
    void onStart() {
        Log.d(TAG, "start");
    }

    public String process(byte[] data, Camera camera) {
        if (!cancel) {
            return null;
        }
//        camera.setOneShotPreviewCallback(null);
//        //处理data
        Camera.Size previewSize = camera.getParameters().getPreviewSize();//获取尺寸,格式转换的时候要用到
        int width = previewSize.width;
        int height = previewSize.height;


        Bitmap bitmap = rawByteArray2RGBABitmap2(data, width, height);
        return barcodeProcessor.process(bitmap);

//        return barcodeProcessor.processBytes(data, 0, 0, width, height);

//        Bitmap bitmap = rawByteArray2RGBABitmap3(data, width, height);
    }

    public Bitmap rawByteArray2RGBABitmap2(byte[] data, int width, int height) {
        int frameSize = width * height;
        int[] rgba = new int[frameSize];
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++) {
                int y = (0xff & ((int) data[i * width + j]));
                int u = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 0]));
                int v = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 1]));
                y = y < 16 ? 16 : y;
                int r = Math.round(1.164f * (y - 16) + 1.596f * (v - 128));
                int g = Math.round(1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = Math.round(1.164f * (y - 16) + 2.018f * (u - 128));
                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);
                rgba[i * width + j] = 0xff000000 + (b << 16) + (g << 8) + r;
            }
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0, width, 0, 0, width, height);
        saveImage(bmp);

        return bmp;
    }

    public Bitmap rawByteArray2RGBABitmap3(byte[] data, int width, int height) {
        Bitmap bmp = null;
        try {
            YuvImage image = new YuvImage(data, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 80, stream);

            bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            //TODO：此处可以对位图进行处理，如显示，保存等
            saveImage(bmp);
            stream.close();
        } catch (Exception ex) {
            Log.e("Sys", "Error:" + ex.getMessage());
        }
        return bmp;
    }

    private boolean flag = false;

    private void saveImage(Bitmap bitmap) {
        if (flag) {
            return;
        }
        flag = true;
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

    public Bitmap rawByteArray2RGBABitmap4(byte[] data, int width, int height) {
        int[] rgba = convertYUV420_NV21toARGB8888(data, width, height);
//        int[] rgba = yuv2rgb(data, width, height);
//        Bitmap bitmap = BitmapFactory.decodeResource(MyApplication.getContext().getResources(), R.drawable.test1);
//        int width = bitmap.getWidth();
//        int height = bitmap.getHeight();
//        int[] rgba = new int[width * height];
//        bitmap.getPixels(rgba, 0, width, 0, 0, width, height);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0, width, 0, 0, width, height);
        saveImage(bmp);

        return bmp;
    }

//    private int[] arrangementArgb(int[] rgba, int width, int heigh) {
////        int[] bitmap = new int[rgba.length];
////        for (int i = 0; i < rgba.length; i++) {
////            bitmap[]
////        }
//    }

    public int[] yuv2rgb(byte[] yuv, int width, int height) {
        int total = width * height;
        int[] rgb = new int[total];
        int Y, Cb = 0, Cr = 0, index = 0, rgb_index = 0;
        int R, G, B;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Y = yuv[y * width + x];
                if (Y < 0) Y += 255;

                if ((x & 1) == 0) {
                    Cr = yuv[(y >> 1) * (width) + x + total];
                    Cb = yuv[(y >> 1) * (width) + x + total + 1];

                    if (Cb < 0) Cb += 127;
                    else Cb -= 128;
                    if (Cr < 0) Cr += 127;
                    else Cr -= 128;
                }

//                Pass
//                R = Math.round(1.164f * (Y - 16) + 1.596f * (Cr - 128));
//                G = Math.round(1.164f * (Y - 16) - 0.813f * (Cr - 128) - 0.391f * (Cb - 128));
//                B = Math.round(1.164f * (Y - 16) + 2.018f * (Cb - 128));

                R = Y + Cr + (Cr >> 2) + (Cr >> 3) + (Cr >> 5);
                G = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1) + (Cr >> 3) + (Cr >> 4) + (Cr >> 5);
                B = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);

                // Approximation
//				R = (int) (Y + 1.40200 * Cr);
//			    G = (int) (Y - 0.34414 * Cb - 0.71414 * Cr);
//				B = (int) (Y + 1.77200 * Cb);

                if (R < 0) R = 0;
                else if (R > 255) R = 255;
                if (G < 0) G = 0;
                else if (G > 255) G = 255;
                if (B < 0) B = 0;
                else if (B > 255) B = 255;

                rgb[index++] = 0xff000000 + (R << 16) + (G << 8) + B;
            }
        }

        return rgb;
    }

    /**
     * Converts YUV420 NV21 to ARGB8888
     *
     * @param data   byte array on YUV420 NV21 format.
     * @param width  pixels width
     * @param height pixels height
     * @return a ARGB8888 pixels int array. Where each int is a pixels ARGB.
     */
    public static int[] convertYUV420_NV21toARGB8888(byte[] data, int width, int height) {
        int size = width * height;
        int offset = size;
        int[] pixels = new int[size];
        int u, v, y1, y2, y3, y4;

        // i along Y and the final pixels
        // k along pixels U and V
        for (int i = 0, k = 0; i < size; i += 2, k += 2) {
            y1 = data[i] & 0xff;
            y2 = data[i + 1] & 0xff;
            y3 = data[width + i] & 0xff;
            y4 = data[width + i + 1] & 0xff;

            u = data[offset + k] & 0xff;
            v = data[offset + k + 1] & 0xff;
            u = u - 128;
            v = v - 128;

            pixels[i] = convertYUVtoARGB(y1, u, v);
            pixels[i + 1] = convertYUVtoARGB(y2, u, v);
            pixels[width + i] = convertYUVtoARGB(y3, u, v);
            pixels[width + i + 1] = convertYUVtoARGB(y4, u, v);

            if (i != 0 && (i + 2) % width == 0)
                i += width;
        }

        return pixels;
    }

    private static int convertYUVtoARGB(int y, int u, int v) {
        int r, g, b;

        r = y + (int) 1.402f * u;
        g = y - (int) (0.344f * v + 0.714f * u);
        b = y + (int) 1.772f * v;
        r = r > 255 ? 255 : r < 0 ? 0 : r;
        g = g > 255 ? 255 : g < 0 ? 0 : g;
        b = b > 255 ? 255 : b < 0 ? 0 : b;
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }


    @Override
    void onStop() {
        Log.d(TAG, "stop");
    }

    @Override
    void onDestroy() {
        Log.d(TAG, "destroy");

        if (baos != null) {
            try {
                baos.close();
                baos = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
