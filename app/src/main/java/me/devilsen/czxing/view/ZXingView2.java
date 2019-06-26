package me.devilsen.czxing.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import cn.bingoogolapple.qrcode.core.BarcodeType;
import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.core.ScanResult;
import cn.bingoogolapple.qrcode.zxing.QRCodeDecoder;
import me.devilsen.czxing.processor.BarcodeProcessor;

/**
 * desc :
 * date : 2019-06-26
 *
 * @author : dongSen
 */
public class ZXingView2 extends QRCodeView {
    private MultiFormatReader mMultiFormatReader;
    private Map<DecodeHintType, Object> mHintMap;

    private BarcodeProcessor barcodeProcessor;


    public ZXingView2(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ZXingView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void setupReader() {
        barcodeProcessor = new BarcodeProcessor();
        mMultiFormatReader = new MultiFormatReader();

//        if (mBarcodeType == BarcodeType.ONE_DIMENSION) {
//            mMultiFormatReader.setHints(QRCodeDecoder.ONE_DIMENSION_HINT_MAP);
//        } else if (mBarcodeType == BarcodeType.TWO_DIMENSION) {
//            mMultiFormatReader.setHints(QRCodeDecoder.TWO_DIMENSION_HINT_MAP);
//        } else if (mBarcodeType == BarcodeType.ONLY_QR_CODE) {
//            mMultiFormatReader.setHints(QRCodeDecoder.QR_CODE_HINT_MAP);
//        } else if (mBarcodeType == BarcodeType.ONLY_CODE_128) {
//            mMultiFormatReader.setHints(QRCodeDecoder.CODE_128_HINT_MAP);
//        } else if (mBarcodeType == BarcodeType.ONLY_EAN_13) {
//            mMultiFormatReader.setHints(QRCodeDecoder.EAN_13_HINT_MAP);
//        } else if (mBarcodeType == BarcodeType.HIGH_FREQUENCY) {
//            mMultiFormatReader.setHints(QRCodeDecoder.HIGH_FREQUENCY_HINT_MAP);
//        } else if (mBarcodeType == BarcodeType.CUSTOM) {
//            mMultiFormatReader.setHints(mHintMap);
//        } else {
//            mMultiFormatReader.setHints(QRCodeDecoder.ALL_HINT_MAP);
//        }
    }

    /**
     * 设置识别的格式
     *
     * @param barcodeType 识别的格式
     * @param hintMap     barcodeType 为 BarcodeType.CUSTOM 时，必须指定该值
     */
    public void setType(BarcodeType barcodeType, Map<DecodeHintType, Object> hintMap) {
        mBarcodeType = barcodeType;
        mHintMap = hintMap;

        if (mBarcodeType == BarcodeType.CUSTOM && (mHintMap == null || mHintMap.isEmpty())) {
            throw new RuntimeException("barcodeType 为 BarcodeType.CUSTOM 时 hintMap 不能为空");
        }
        setupReader();
    }

    @Override
    protected ScanResult processBitmapData(Bitmap bitmap) {
        return new ScanResult(QRCodeDecoder.syncDecodeQRCode(bitmap));
    }

    @Override
    protected ScanResult processData(byte[] data, int width, int height, boolean isRetry) {

//        Bitmap bitmap = rawByteArray2RGBABitmap2(data, width, height);

//        String result = barcodeProcessor.process(bitmap);
        String result = barcodeProcessor.processBytes(data, 0, 0, width, height);
//        String result = rawResult.getText();
        if (TextUtils.isEmpty(result)) {
            Log.e("Scan >>>", "no code");
            return null;
        } else {
            Log.e("Scan >>>", result);
        }

        return new ScanResult(result);
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

            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)) {
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


    private boolean isNeedAutoZoom(BarcodeFormat barcodeFormat) {
        return isAutoZoom() && barcodeFormat == BarcodeFormat.QR_CODE;
    }
}