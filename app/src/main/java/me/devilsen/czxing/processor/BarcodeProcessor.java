package me.devilsen.czxing.processor;

import android.graphics.Bitmap;

import me.devilsen.czxing.BarCodeUtil;
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
                BarcodeFormat.QR_CODE
//                BarcodeFormat.AZTEC,
//                BarcodeFormat.CODABAR,
//                BarcodeFormat.CODE_39,
//                BarcodeFormat.CODE_93,
//                BarcodeFormat.CODE_128,
//                BarcodeFormat.DATA_MATRIX,
//                BarcodeFormat.EAN_8,
//                BarcodeFormat.EAN_13,
//                BarcodeFormat.ITF,
//                BarcodeFormat.MAXICODE,
//                BarcodeFormat.PDF_417,
//                BarcodeFormat.RSS_14,
//                BarcodeFormat.RSS_EXPANDED,
//                BarcodeFormat.UPC_A,
//                BarcodeFormat.UPC_E,
//                BarcodeFormat.UPC_EAN_EXTENSION);
        );
    }

    @Override
    void onStart() {
    }

    public String process(final Bitmap bitmap) {
        if (cancel) {
            return null;
        }

        BarcodeReader.Result result = reader.read(bitmap, bitmap.getWidth(), bitmap.getHeight());
        if (result != null) {
            return result.getText();
        }
        return null;
    }

    public BarcodeReader.Result processBytes(byte[] data, int cropLeft, int cropTop, int cropWidth, int cropHeight, int rowWidth) {
        if (cancel) {
            return null;
        }

        long start = System.currentTimeMillis();

        BarcodeReader.Result result = reader.read(data, cropLeft, cropTop, cropWidth, cropHeight, rowWidth);
        if (result != null) {
            BarCodeUtil.d("have Code reader time: " + (System.currentTimeMillis() - start));
            return result;
        }
        BarCodeUtil.d("no Code reader time: " + (System.currentTimeMillis() - start));

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
