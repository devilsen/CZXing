package me.devilsen.czxing.view;

import me.devilsen.czxing.code.BarcodeFormat;

/**
 * @author : dongSen
 * date : 2019-06-29 16:16
 * desc :
 */
public interface ScanListener {

    /**
     * 扫描结果
     *
     * @param result 摄像头扫码时只要回调了该方法 result 就一定有值，不会为 null
     */
    void onScanSuccess(String result, BarcodeFormat format);

    /**
     * 处理打开相机出错
     */
    void onOpenCameraError();

    /**
     * 亮度分析回调
     */
    interface AnalysisBrightnessListener {

        void onAnalysisBrightness(boolean isDark);

    }
}