package me.devilsen.czxing.view.scanview;

import android.content.Context;
import android.util.AttributeSet;

import java.util.List;

import me.devilsen.czxing.code.BarcodeDecoder;
import me.devilsen.czxing.code.BarcodeFormat;
import me.devilsen.czxing.code.CodeResult;

/**
 * @author : dongSen
 * date : 2019-06-29 16:18
 * desc : 二维码界面使用类
 */
public class DetectView extends CameraView {

    private BarcodeDecoder mDecoder;
    private boolean isStop;
    private BarcodeDecoder.OnDetectCodeListener mDetectCodeListener;
    private BarcodeDecoder.OnDetectBrightnessListener mDetectBrightnessListener;
    private BarcodeDecoder.OnFocusListener mFocusListener;

    public DetectView(Context context) {
        this(context, null);
    }

    public DetectView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DetectView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDecoder = new BarcodeDecoder();
    }

    @Override
    public void onPreviewFrame(byte[] data, int rowWidth, int rowHeight) {
        if (isStop) {
            return;
        }

        mDecoder.decodeYUVASync(data, 0, 0, rowWidth, rowHeight, rowWidth, rowHeight, new BarcodeDecoder.OnDetectCodeListener() {
            @Override
            public void onReadCodeResult(List<CodeResult> resultList) {
                if (resultList.size() == 0) {
                    return;
                }

                if (!isStop && mDetectCodeListener != null) {
                    mDetectCodeListener.onReadCodeResult(resultList);
                }
            }
        }, new BarcodeDecoder.OnFocusListener() {
            @Override
            public void onFocus() {
                if (!isStop && mFocusListener != null) {
                    mFocusListener.onFocus();
                }
            }
        });

        mDecoder.detectBrightnessASync(data, rowWidth, rowHeight, new BarcodeDecoder.OnDetectBrightnessListener() {
            @Override
            public void onAnalysisBrightness(double brightness) {
                if (!isStop && mDetectBrightnessListener != null) {
                    mDetectBrightnessListener.onAnalysisBrightness(brightness);
                }
            }
        });
    }

    /**
     * 设置扫描格式
     */
    public void setBarcodeFormat(BarcodeFormat... formats) {
        mDecoder.setBarcodeFormat(formats);
    }

    public void startDetect() {
        isStop = false;
    }

    public void stopDetect() {
        isStop = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isStop = true;
        if (mDecoder != null) {
            mDecoder.destroy();
            mDecoder = null;
        }
    }

    public void setOnDetectCodeListener(BarcodeDecoder.OnDetectCodeListener listener) {
        this.mDetectCodeListener = listener;
    }

    public void setOnDetectBrightnessListener(BarcodeDecoder.OnDetectBrightnessListener listener) {
        this.mDetectBrightnessListener = listener;
    }

    public void setOnFocusListener(BarcodeDecoder.OnFocusListener listener) {
        this.mFocusListener = listener;
    }

    public void setDetectModel(String detectorPrototxtPath, String detectorCaffeModelPath, String superResolutionPrototxtPath, String superResolutionCaffeModelPath) {
        if (mDecoder != null) {
            mDecoder.setDetectModel(detectorPrototxtPath, detectorCaffeModelPath, superResolutionPrototxtPath, superResolutionCaffeModelPath);
        }
    }
}
