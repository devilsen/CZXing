package me.devilsen.czxing.processor;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import me.devilsen.czxing.BarcodeFormat;
import me.devilsen.czxing.BarcodeReader;

/**
 * desc : 二维码处理模块
 * date : 2019-06-25
 *
 * @author : dongSen
 */
class BarcodeProcessor extends Processor {

    private static final String TAG = "Scan >>> ";
    private BarcodeReader reader;

    BarcodeProcessor() {
        reader = new BarcodeReader(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.AZTEC,
                BarcodeFormat.CODABAR,
                BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.CODE_128,
                BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.EAN_8,
                BarcodeFormat.EAN_13,
                BarcodeFormat.ITF,
                BarcodeFormat.MAXICODE,
                BarcodeFormat.PDF_417,
                BarcodeFormat.RSS_14,
                BarcodeFormat.RSS_EXPANDED,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.UPC_EAN_EXTENSION);
    }

    @Override
    void onStart() {
    }

    String process(final Bitmap bitmap) {
//        if (!mSwitch) {
//            return null;
//        }
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                saveImage(bitmap);
            }
        }).start();

        BarcodeReader.Result result = reader.read(bitmap, bitmap.getWidth(), bitmap.getHeight());

        if (result != null) {
            Log.d(TAG, "format: " + result.getFormat() + " text: " + result.getText());
            return result.getText();
        }
        return null;
    }

    String processBytes(byte[] data, int cropWidth, int cropHeight, int imgWidth, int imgHeight) {
        BarcodeReader.Result result = reader.read(data, cropWidth, cropHeight, imgWidth, imgHeight);
        if (result != null) {
            Log.d(TAG, "format: " + result.getFormat() + " text: " + result.getText());
            return result.getText();
        }
        return null;
    }

    private void saveImage(Bitmap bitmap) {
        String thumbPath = System.currentTimeMillis() + ".png";
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

            if (bitmap.compress(Bitmap.CompressFormat.PNG, 80, out)) {
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

    @Override
    void onStop() {
    }

    @Override
    void onDestroy() {
        reader = null;
    }
}
