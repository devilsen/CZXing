//
// Created by Devilsen on 2019-08-09.
//

#ifndef CZXING_IMAGESCHEDULER_H
#define CZXING_IMAGESCHEDULER_H

#include <jni.h>
#include <opencv2/core/mat.hpp>
#include <opencv2/opencv.hpp>
#include <opencv2/wechat_qrcode.hpp>
#include <src/MultiFormatReader.h>
#include <src/BinaryBitmap.h>
#include <src/DecodeHints.h>
#include "ScanResult.h"
#include "config.h"
#include <memory.h>

CZXING_BEGIN_NAMESPACE()
USING_CZXING_NAMESPACE()

class ImageScheduler {
public:
    ImageScheduler();

    ~ImageScheduler();

    /**
     * 解析相机数据
     */
    std::vector<ScanResult> readByte(JNIEnv *env, jbyte *bytes, int width, int height);

    /**
     * 解析图片数据
     */
    std::vector<ScanResult> readBitmap(JNIEnv *env, jobject bitmap);

    void setFormat(JNIEnv *env, jintArray formats);

    void setWeChatDetect(const char* detectorPrototxtPath, const char* detectorCaffeModelPath,
                         const char* superResolutionPrototxtPath, const char* superResolutionCaffeModelPath);

private:
    ZXing::DecodeHints m_formatHints;

    cv::wechat_qrcode::WeChatQRCode m_weChatQrCodeReader;

    std::vector<ScanResult> m_defaultResult;
    double m_CameraLight { 0 };

    std::vector<ScanResult> startRead(const cv::Mat &gray);

    std::vector<ScanResult> decodeWeChat(const cv::Mat &gray);

    std::vector<ScanResult> decodeThresholdPixels(const cv::Mat &gray);

    std::vector<ScanResult> decodeAdaptivePixels(const cv::Mat &gray);

    std::vector<ScanResult> zxingDecode(const cv::Mat &mat);

    double analysisBrightness(const cv::Mat &gray);

    std::vector<ScanResult> defaultResult();

    bool onlyQrCode();

    bool containQrCode();
};


CZXING_END_NAMESPACE()

#endif //CZXING_IMAGESCHEDULER_H
