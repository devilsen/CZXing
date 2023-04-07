//
// Created by Devilsen on 2019-08-09.
//

#ifndef CZXING_DECODESCHEDULER_H
#define CZXING_DECODESCHEDULER_H

#include <jni.h>
#include <opencv2/core/mat.hpp>
#include <opencv2/opencv.hpp>
#include <opencv2/wechat_qrcode.hpp>
#include <src/MultiFormatReader.h>
#include <src/BinaryBitmap.h>
#include <src/DecodeHints.h>
#include "ScanResult.h"
#include "config.h"
#include "Mutex.h"
#include <memory.h>

CZXING_BEGIN_NAMESPACE()

class DecodeScheduler {
public:
    DecodeScheduler() { m_formatHints.setFormats(ZXing::BarcodeFormat::QRCode); }
    ~DecodeScheduler() = default;

    /**
     * 解析相机数据
     */
    std::vector<ScanResult> readByte(jbyte *bytes, int width, int height);

    /**
     * 解析图片数据
     */
    std::vector<ScanResult> readBitmap(JNIEnv *env, jobject bitmap);

    /** 探测画面亮度 */
    double detectBrightness(jbyte *bytes, int width, int height);

    void setFormat(JNIEnv *env, jintArray formats);

    void setWeChatDetect(const char* detectorPrototxtPath, const char* detectorCaffeModelPath,
                         const char* superResolutionPrototxtPath, const char* superResolutionCaffeModelPath);

private:
    ZXing::DecodeHints m_formatHints;
    cv::wechat_qrcode::WeChatQRCode m_weChatQrCodeReader;
    std::vector<ScanResult> m_defaultResult;
    double m_CameraLight { 0 };
    bool m_onlyQrCode;
    bool m_containQrCode;
    Mutex m_decodeLock;

    std::vector<ScanResult> startDecode(const cv::Mat &gray);
    std::vector<ScanResult> decodeWeChat(const cv::Mat &gray);
    std::vector<ScanResult> decodeThresholdPixels(const cv::Mat &gray);
    std::vector<ScanResult> decodeAdaptivePixels(const cv::Mat &gray);
    std::vector<ScanResult> zxingDecode(const cv::Mat &mat);
    double analysisBrightness(const cv::Mat &mat);
};


CZXING_END_NAMESPACE()

#endif //CZXING_DECODESCHEDULER_H
