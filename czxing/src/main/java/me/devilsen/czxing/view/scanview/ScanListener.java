package me.devilsen.czxing.view.scanview;

import androidx.annotation.NonNull;

import java.util.List;

import me.devilsen.czxing.code.CodeResult;

/**
 * @author : dongSen
 * date : 2019-06-29 16:16
 * desc :
 */
public interface ScanListener {

    /**
     * 扫描结果
     *
     * @param resultList 摄像头扫码时只要回调了该方法 result 就一定有值
     */
    void onScanSuccess(@NonNull List<CodeResult> resultList);

    /**
     * 点击扫码结果，当有多个二维码结果时，需要点击触发结果
     *
     * @param result 扫码结果
     */
    void onClickResult(CodeResult result);

    /**
     * 处理打开相机出错
     */
    void onOpenCameraError();

    /**
     * 亮度分析回调
     */
    interface AnalysisBrightnessListener {

        void onAnalysisBrightness(double brightness);

    }
}