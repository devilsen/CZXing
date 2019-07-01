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
import me.devilsen.czxing.SaveImageUtil;
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

        String result = barcodeProcessor.processBytes(data, 200, 200, 600, 600, 1080);

        SaveImageUtil.byteArray2Bitmap(data, 100, 100, 600, 1000, 1080);
//        String result = "";

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

    public Bitmap rawByteArray2RGBABitmap4(byte[] data, int left, int top, int width, int height, int rowWidth) {
//        int[] rgba = applyGrayScale(data, width, height);
        int[] rgba = applyGrayScale(data, left, top, width, height, rowWidth);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0, width, 0, 0, width, height);
        saveImage(bmp);

        return bmp;
    }

    public static int[] applyGrayScale(byte[] data, int width, int height) {
        int p;
        int[] pixels = new int[width * height];
        int size = width * height;
        for (int i = 0; i < size; i++) {
            p = data[i] & 0xFF;
            pixels[i] = 0xff000000 | p << 16 | p << 8 | p;
        }
        return pixels;
    }

    public static int[] applyGrayScale(byte[] data, int left, int top, int width, int height, int rowWidth) {
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

    public static int[] convertYUV420_NV21toRGB8888(byte[] data, int width, int height) {
        int size = width * height;
        int offset = size;
        int[] pixels = new int[size];
        int u, v, y1, y2, y3, y4;

        // i percorre os Y and the final pixels
        // k percorre os pixles U e V
        for (int i = 0, k = 0; i < size; i += 2, k += 2) {
            y1 = data[i] & 0xff;
            y2 = data[i + 1] & 0xff;
            y3 = data[width + i] & 0xff;
            y4 = data[width + i + 1] & 0xff;

            u = data[offset + k] & 0xff;
            v = data[offset + k + 1] & 0xff;
            u = u - 128;
            v = v - 128;

            pixels[i] = convertYUVtoRGB(y1, u, v);
            pixels[i + 1] = convertYUVtoRGB(y2, u, v);
            pixels[width + i] = convertYUVtoRGB(y3, u, v);
            pixels[width + i + 1] = convertYUVtoRGB(y4, u, v);

            if (i != 0 && (i + 2) % width == 0)
                i += width;
        }

        return pixels;
    }

    private static int convertYUVtoRGB(int y, int u, int v) {
        int r, g, b;

        r = y + (int) 1.402f * v;
        g = y - (int) (0.344f * u + 0.714f * v);
        b = y + (int) 1.772f * u;
        r = r > 255 ? 255 : r < 0 ? 0 : r;
        g = g > 255 ? 255 : g < 0 ? 0 : g;
        b = b > 255 ? 255 : b < 0 ? 0 : b;
        return 0xff000000 | (b << 16) | (g << 8) | r;
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