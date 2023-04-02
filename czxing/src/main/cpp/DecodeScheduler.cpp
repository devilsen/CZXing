//
// Created by Devilsen on 2019-08-09.
//

#include <src/ReadBarcode.h>
#include <opencv2/wechat_qrcode.hpp>
#include <opencv2/imgproc.hpp>
#include <memory>
#include "DecodeScheduler.h"
#include "ScanResult.h"
#include "JNIUtils.h"
#include "MatUtils.h"
#include "JniHelper.h"

#define DEFAULT_MIN_LIGHT 30

USING_CZXING_NAMESPACE()

void DecodeScheduler::setFormat(JNIEnv* env, jintArray formats_)
{
    if (formats_ == nullptr) return;

    std::vector<ZXing::BarcodeFormat> formats = GetFormats(env, formats_);
    if (!formats.empty()) {
        m_formatHints.setPossibleFormats(formats);

        for (auto& format : formats) {
            LOGE("set format: %d", format)
        }
    }

    m_onlyQrCode = false;
    m_containQrCode = false;

    if (m_formatHints.hasFormat(ZXing::BarcodeFormat::QRCode)) {
        if (m_formatHints.formats().count() == 1) {
            m_onlyQrCode = true;
        } else {
            m_containQrCode = true;
        }
    }
}

void DecodeScheduler::setWeChatDetect(const char* detectorPrototxtPath,
                                      const char* detectorCaffeModelPath,
                                      const char* superResolutionPrototxtPath,
                                      const char* superResolutionCaffeModelPath)
{
    try {
        LOGE("wechat_qrcode set model, detectorPrototxtPath = %s", detectorPrototxtPath)
        LOGE("wechat_qrcode set model, detectorCaffeModelPath = %s", detectorCaffeModelPath)
        LOGE("wechat_qrcode set model, superResolutionPrototxtPath = %s",superResolutionPrototxtPath)
        LOGE("wechat_qrcode set model, superResolutionCaffeModelPath = %s",superResolutionCaffeModelPath)

        m_weChatQrCodeReader = cv::wechat_qrcode::WeChatQRCode(detectorPrototxtPath,
                                                               detectorCaffeModelPath,
                                                               superResolutionPrototxtPath,
                                                               superResolutionCaffeModelPath);
    } catch (const std::exception& e) {
        LOGE("wechat_qrcode init exception = %s", e.what())
    }
}

std::vector<ScanResult>
DecodeScheduler::readByte(jbyte* bytes, int width, int height)
{
    LOGE("start readByte rowWidth = %d rowHeight = %d", width, height)

    cv::Mat src(height + height / 2, width, CV_8UC1, bytes);
    saveMat(src);

    // 转灰并更改格式
    cv::Mat gray;
    cvtColor(src, gray, cv::COLOR_YUV2GRAY_NV21);
    saveMat(gray, "gray");

    // 顺时针旋转90度图片，得到正常的图片（Android的后置摄像头获取的格式是横着的，需要旋转90度）
    rotate(gray, gray, cv::ROTATE_90_CLOCKWISE);

    LOGE("end readByte rowWidth = %d rowHeight = %d", width, height)
    return startDecode(gray);
}

std::vector<ScanResult> DecodeScheduler::readBitmap(JNIEnv* env, jobject bitmap)
{
    cv::Mat src;
    BitmapToMat(env, bitmap, src);
    saveMat(src, "src");

    cv::Mat gray;
    cvtColor(src, gray, cv::COLOR_RGBA2GRAY);
    saveMat(gray, "gray");

    return decodeWeChat(gray);
}

std::vector<ScanResult> DecodeScheduler::startDecode(const cv::Mat& gray)
{
    // 分析亮度，如果亮度过低，不进行处理
    if (analysisBrightness(gray) < DEFAULT_MIN_LIGHT) {
        return m_defaultResult;
    }

    // 扫码格式只有二维码
    if (m_onlyQrCode) {
        return decodeWeChat(gray);
    } else if (m_containQrCode) { // 包含二维码
        std::vector<ScanResult> result = decodeWeChat(gray);
        if (result.empty()) {
            return decodeThresholdPixels(gray);
        }
        return result;
    } else { // 没有二维码
        return decodeThresholdPixels(gray);
    }
}

std::vector<ScanResult> DecodeScheduler::decodeWeChat(const cv::Mat& gray)
{
    LOGE("start wechat decode")
    std::vector<cv::Mat> points;
    std::vector<std::string> res = m_weChatQrCodeReader.detectAndDecode(gray, points);
    if (res.empty()) {
        return m_defaultResult;
    }
    LOGE("Yes! wechat get the result")

    std::vector<ScanResult> resultVector;
    for (int i = 0; i < res.size(); ++i) {
        cv::Rect rect = cv::boundingRect(points[i]);

        CodeRect codeRect(rect.x, rect.y , rect.width, rect.height);
        ScanResult result(res[i], codeRect);
        resultVector.push_back(result);

        LOGE("result = %s", res[i].c_str())
        LOGE("rect: x = %d, y = %d, width = %d, height = %d", rect.x, rect.y, rect.width, rect.height)
    }

    return resultVector;
}

/**
 * 2. 如果gray化没有解析出来，尝试
 * 提升亮度，处理图片亮度过低时的情况
 * 二值化处理，让二维码更清晰
 * 旋转90度
 *
 * @param gray
 */
std::vector<ScanResult> DecodeScheduler::decodeThresholdPixels(const cv::Mat& mat)
{
    LOGE("start ThresholdPixels...")

    // 提升亮度
    if (m_CameraLight < 80) {
        mat.convertTo(mat, -1, 1.0, 30);
    }

    threshold(mat, mat, 50, 255, cv::THRESH_OTSU);

    std::vector<ScanResult> result = zxingDecode(mat);
    if (result.empty()) {
        return decodeAdaptivePixels(mat);
    } else {
        return result;
    }
}

/**
 * 3. 降低图片亮度，再次识别图像，处理亮度过高时的情况
 * 逆时针旋转90度，即旋转了270度
 * @param gray
 */
std::vector<ScanResult> DecodeScheduler::decodeAdaptivePixels(const cv::Mat& mat)
{
    LOGE("start AdaptivePixels...")

    // 降低图片亮度
    cv::Mat lightMat;
    mat.convertTo(lightMat, -1, 1.0, -60);

    adaptiveThreshold(lightMat, lightMat, 255, cv::ADAPTIVE_THRESH_MEAN_C,
                      cv::THRESH_BINARY, 55, 3);

    return zxingDecode(lightMat);
}

std::vector<ScanResult> DecodeScheduler::zxingDecode(const cv::Mat& mat)
{
    LOGE("start zxing decode")

    ZXing::ImageView imageView(mat.data, mat.cols, mat.rows, ZXing::ImageFormat::Lum);
    ZXing::Result result = ZXing::ReadBarcode(imageView, m_formatHints);
    if (result.isValid()) {
        LOGE("zxing decode success, result data = %ls", result.text().c_str())
        std::vector<ScanResult> vector;

        int zxingFormatIndex = static_cast<int>(result.format());
        auto format = static_cast<CodeFormat>(zxingFormatIndex);

        ZXing::Position position = result.position();
        CodeRect codeRect(position.topLeft().x, position.topLeft().y, position.bottomRight().x, position.bottomRight().y);

        ScanResult scanResult(format, UnicodeToANSI(result.text()), codeRect);
        vector.push_back(scanResult);
        return vector;
    }
    return m_defaultResult;
}

double DecodeScheduler::detectBrightness(jbyte* bytes, int width, int height)
{
    cv::Mat src(height + height / 2, width, CV_8UC1, bytes);
    return analysisBrightness(src);
}

double DecodeScheduler::analysisBrightness(const cv::Mat& mat)
{
    // 平均亮度
    m_CameraLight = mean(mat).val[0];
//    LOGE("平均亮度 %lf", m_CameraLight)
    return m_CameraLight;
}
