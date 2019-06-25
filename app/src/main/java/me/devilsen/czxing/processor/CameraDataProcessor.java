package me.devilsen.czxing.processor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;

import java.io.ByteArrayOutputStream;
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
        if (!mSwitch) {
            return null;
        }

//        camera.setOneShotPreviewCallback(null);
//        //处理data
        Camera.Size previewSize = camera.getParameters().getPreviewSize();//获取尺寸,格式转换的时候要用到
        int width = previewSize.width;
        int height = previewSize.height;
//        BitmapFactory.Options newOpts = new BitmapFactory.Options();
//        newOpts.inJustDecodeBounds = true;
//        YuvImage yuvimage = new YuvImage(
//                data,
//                ImageFormat.NV21,
//                previewSize.width,
//                previewSize.height,
//                null);
//        baos = new ByteArrayOutputStream();
//        yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 80, baos);// 80--JPG图片的质量[0-100],100最高
//        byte[] rawImage = baos.toByteArray();
//        //将rawImage转换成bitmap
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inPreferredConfig = Bitmap.Config.RGB_565;
//        Bitmap bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);

//        PlanarYUVLuminanceSource source = null;
//        source = new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false);
//        Bitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
//

        return barcodeProcessor.processBytes(data, 0, 0, width, height);
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
