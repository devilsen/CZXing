package me.devilsen.czxing.util;

import android.media.Image;
import android.media.ImageReader;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Helper {

    /***
     * ImageReader中读取YUV
     */
    @Nullable
    public static byte[] readYuv(ImageReader reader) {
        Image image;
        image = reader.acquireLatestImage();
        if (image == null) {
            return null;
        }
//        BarCodeUtil.d("image width = " + image.getWidth() + " height = " + image.getHeight());
        byte[] data = getByteFromImage(image);
        image.close();
        return data;
    }

    @Nullable
    private static byte[] getByteFromImage(Image image) {
        byte[] nv21 = null;
        try {
            if (image == null || image.getPlanes() == null || image.getPlanes().length == 0)
                return null;
            Image.Plane[] planes = image.getPlanes();
            int remaining0 = planes[0].getBuffer().remaining();
            int remaining2 = planes[2].getBuffer().remaining();
            int w = image.getWidth();
            int h = image.getHeight();
            byte[] yRawSrcBytes = new byte[remaining0];
            byte[] uvRawSrcBytes = new byte[remaining2];
            nv21 = new byte[w * h * 3 / 2];
            planes[0].getBuffer().get(yRawSrcBytes);
            planes[2].getBuffer().get(uvRawSrcBytes);
            //0b10000001 对应-127,YUV灰度操作
//            for (int i = 0; i < uvRawSrcBytes.length; i++)
//                nv21[yRawSrcBytes.length + i] = (byte) 0b10000001;
            for (int i = 0; i < h; i++) {
                System.arraycopy(yRawSrcBytes, planes[0].getRowStride() * i,
                        nv21, w * i, w);

                if (i > image.getHeight() / 2)
                    continue;

                int offset = w * (h + i);

                if (offset + w >= nv21.length)
                    continue;

                System.arraycopy(uvRawSrcBytes, planes[2].getRowStride() * i,
                        nv21, offset, w);
            }
            return nv21;
        } catch (Exception e) {
            return nv21;
        }
    }

}
