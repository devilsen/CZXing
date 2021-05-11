//
// Created by Devilsen on 2019-08-09.
//

#ifndef CZXING_IMAGESCHEDULER_H
#define CZXING_IMAGESCHEDULER_H

#include <jni.h>
#include <unistd.h>
#include <opencv2/core/mat.hpp>
#include <opencv2/opencv.hpp>
#include <src/MultiFormatReader.h>
#include <src/BinaryBitmap.h>
#include <src/DecodeHints.h>
#include "zbar/zbar.h"
#include "Result.h"
#include "JNIUtils.h"

class ImageScheduler {
public:
    ImageScheduler(const ZXing::DecodeHints &hints);

    ~ImageScheduler();

    ZXing::Result
    readByte(JNIEnv *env, jbyte *bytes, int left, int top, int width, int height, int rowWidth,
             int rowHeight);

    ZXing::Result
    readBitmap(JNIEnv *env, jobject bitmap, int left, int top, int width, int height);

    ZXing::Result readBitmap(JNIEnv *env, jobject bitmap);

    void setFormat(JNIEnv *env, jintArray formats);

    void setWeChatDetect(const char* detectorPrototxtPath, const char* detectorCaffeModelPath,
                         const char* superResolutionPrototxtPath, const char* superResolutionCaffeModelPath);

private:
    ZXing::MultiFormatReader *reader;
    zbar::ImageScanner *zbarScanner;
    cv::wechat_qrcode::WeChatQRCode *m_weChatQrCode;
    double m_CameraLight;
    unsigned int m_FileIndex;

    ZXing::Result startRead(const cv::Mat &gray, int dataType);

    ZXing::Result decodeWeChat(const cv::Mat &gray, int dataType);

    ZXing::Result decodeZBar(const cv::Mat &gray, int dataType);

    ZXing::Result decodeThresholdPixels(const cv::Mat &gray, int dataType);

    ZXing::Result decodeAdaptivePixels(const cv::Mat &gray, int dataType);

    ZXing::Result decodeNegative(const cv::Mat &gray, int dataType);

    double analysisBrightness(const cv::Mat &gray);

    ZXing::Result zxingDecode(const cv::Mat &mat, int dataType);

    ZXing::Result zbarDecode(const cv::Mat &mat);

    ZXing::Result zbarDecode(const void *raw, unsigned int width, unsigned int height);

    void logDecode(int scanType, int treatType);

    void saveMat(const cv::Mat &mat, const std::string &fileName = "src");

    void saveIncreaseMat(const cv::Mat &mat);
};

#endif //CZXING_IMAGESCHEDULER_H
