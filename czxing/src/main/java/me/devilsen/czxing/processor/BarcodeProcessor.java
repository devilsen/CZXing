package me.devilsen.czxing.processor;

import android.graphics.Bitmap;

import me.devilsen.czxing.code.BarcodeFormat;
import me.devilsen.czxing.code.BarcodeReader;
import me.devilsen.czxing.code.CodeResult;

/**
 * desc : 二维码处理模块
 * date : 2019-06-25
 *
 * @author : dongSen
 */
public class BarcodeProcessor {

    private BarcodeReader reader;
    private boolean cancel;

    public BarcodeProcessor() {
        reader = BarcodeReader.getInstance();
        reader.setBarcodeFormat(
                BarcodeFormat.QR_CODE,
//                BarcodeFormat.AZTEC,
                BarcodeFormat.CODABAR,
//                BarcodeFormat.CODE_39,
//                BarcodeFormat.CODE_93,
                BarcodeFormat.CODE_128,
//                BarcodeFormat.DATA_MATRIX,
//                BarcodeFormat.EAN_8,
                BarcodeFormat.EAN_13,
//                BarcodeFormat.ITF,
//                BarcodeFormat.MAXICODE,
//                BarcodeFormat.PDF_417,
//                BarcodeFormat.RSS_14,
//                BarcodeFormat.RSS_EXPANDED,
                BarcodeFormat.UPC_A
//                BarcodeFormat.UPC_E,
//                BarcodeFormat.UPC_EAN_EXTENSION
        );
    }

    public String process(final Bitmap bitmap) {
        if (cancel) {
            return null;
        }

        CodeResult result = reader.read(bitmap);
        if (result != null) {
            return result.getText();
        }
        return null;
    }

    public CodeResult processBytes(byte[] data, int cropLeft, int cropTop, int cropWidth, int cropHeight, int rowWidth, int rowHeight) {
        if (cancel) {
            return null;
        }

        CodeResult result = reader.read(data, cropLeft, cropTop, cropWidth, cropHeight, rowWidth, rowHeight);
        if (result != null) {
            return result;
        }
        return null;
    }

    /**
     * 分析亮度
     *
     * @param data        摄像头数据
     * @param imageWidth  图像宽度
     * @param imageHeight 图像高度
     * @return 是否过暗
     */
    public boolean analysisBrightness(byte[] data, int imageWidth, int imageHeight) {
        if (cancel) {
            return false;
        }
        return false;
    }

    public void setReadCodeListener(BarcodeReader.ReadCodeListener readCodeListener) {
        reader.setReadCodeListener(readCodeListener);
    }

    public void cancel() {
        cancel = true;
    }

}
