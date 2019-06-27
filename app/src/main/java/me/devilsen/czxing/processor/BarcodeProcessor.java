package me.devilsen.czxing.processor;

import android.graphics.Bitmap;
import android.util.Log;

import me.devilsen.czxing.BarcodeFormat;
import me.devilsen.czxing.BarcodeReader;

/**
 * desc : 二维码处理模块
 * date : 2019-06-25
 *
 * @author : dongSen
 */
public class BarcodeProcessor extends Processor {

    private static final String TAG = "Scan >>> ";
    private BarcodeReader reader;

    public BarcodeProcessor() {
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

    public String process(final Bitmap bitmap) {
        if (mSwitch) {
            return null;
        }
        mSwitch = true;

//        new Thread(() -> saveImage(bitmap)).start();

        BarcodeReader.Result result = reader.read(bitmap, bitmap.getWidth(), bitmap.getHeight());

        if (result != null) {
            Log.d(TAG, "format: " + result.getFormat() + " text: " + result.getText());
            return result.getText();
        } else {
            Log.d(TAG, "no Code");
        }
        mSwitch = false;
        return null;
    }

    public String processBytes(byte[] data, int cropWidth, int cropHeight, int imgWidth, int imgHeight) {
        if (mSwitch) {
            return null;
        }
        mSwitch = true;
        BarcodeReader.Result result = reader.read(data, cropWidth, cropHeight, imgWidth, imgHeight);
        if (result != null) {
            Log.d(TAG, "format: " + result.getFormat() + " text: " + result.getText());
            return result.getText();
        } else {
            Log.d(TAG, "no Code");
        }
        mSwitch = false;
        return null;
    }

    @Override
    void onStop() {
    }

    @Override
    void onDestroy() {
        reader = null;
    }
}
