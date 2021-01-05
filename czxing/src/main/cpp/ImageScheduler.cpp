//
// Created by Devilsen on 2019-08-09.
//

#include "ImageScheduler.h"

#define DEFAULT_MIN_LIGHT 30

#define SCAN_ZXING 0
#define SCAN_ZBAR 1

#define DATA_TYPE_BITMAP 0
#define DATA_TYPE_BYTE 1

#define SCAN_TREAT_GRAY 0
#define SCAN_TREAT_THRESHOLD 1
#define SCAN_TREAT_ADAPTIVE 2
#define SCAN_TREAT_NEGATIVE 3

ImageScheduler::ImageScheduler(const ZXing::DecodeHints &hints) {
    reader = new ZXing::MultiFormatReader(hints);
    zbarScanner = new zbar::ImageScanner();
    zbarScanner->set_config(zbar::ZBAR_QRCODE, zbar::ZBAR_CFG_ENABLE, 1);

    m_CameraLight = 0;
    m_FileIndex = 0;
}

ImageScheduler::~ImageScheduler() {
    DELETE(reader)
    DELETE(zbarScanner)
}

ZXing::Result
ImageScheduler::readByte(JNIEnv *env, jbyte *bytes, int left, int top, int cropWidth,
                         int cropHeight,
                         int rowWidth,
                         int rowHeight) {

    LOGE("save left = %d top = %d width = %d height = %d rowWidth = %d rowHeight = %d",
         left, top, cropWidth, cropHeight, rowWidth, rowHeight)

    cv::Mat src(rowHeight + rowHeight / 2, rowWidth,
                CV_8UC1, bytes);
//        saveMatSrc(src);

    // 转灰并更改格式
    cv::Mat gray;
    cvtColor(src, gray, cv::COLOR_YUV2GRAY_NV21);

    // 截取
    if (left != 0) {
        gray = gray(cv::Rect(left, top, cropWidth, cropHeight));
    }
//        saveMat(gray);

    // 顺时针旋转90度图片，得到正常的图片（Android的后置摄像头获取的格式是横着的，需要旋转90度）
    rotate(gray, gray, cv::ROTATE_90_CLOCKWISE);

    return startRead(gray, DATA_TYPE_BYTE);
}

ZXing::Result
ImageScheduler::readBitmap(JNIEnv *env, jobject bitmap, int left, int top, int cropWidth,
                           int cropHeight) {
    cv::Mat src;
    BitmapToMat(env, bitmap, src);

    cv::Mat gray;
    cvtColor(src, gray, cv::COLOR_RGBA2GRAY);

    // 截取
    if (left != 0) {
        gray = gray(cv::Rect(left, top, cropWidth, cropHeight));
    }

    return startRead(gray, DATA_TYPE_BITMAP);
}

ZXing::Result ImageScheduler::readBitmap(JNIEnv *env, jobject bitmap) {
    cv::Mat src;
    BitmapToMat(env, bitmap, src);
    saveMat(src, "src");

    cv::Mat gray;
    cvtColor(src, gray, cv::COLOR_RGBA2GRAY);
    saveMat(gray, "gray");

    return startRead(gray, DATA_TYPE_BITMAP);
}

void ImageScheduler::setFormat(JNIEnv *env, jintArray formats_) {
    ZXing::DecodeHints hints;
    if (formats_ != nullptr) {
        std::vector<ZXing::BarcodeFormat> formats = GetFormats(env, formats_);
        hints.setPossibleFormats(formats);
    }
    reader->setFormat(hints);
}

/**
 * 0. 预处理二进制数据
 */
ZXing::Result ImageScheduler::startRead(const cv::Mat &gray, int dataType) {
    // 分析亮度，如果亮度过低，不进行处理
    if (analysisBrightness(gray) < DEFAULT_MIN_LIGHT) {
        return ZXing::Result(ZXing::DecodeStatus::TooDark);
    }

//    return decodeZBar(gray, dataType);
    return decodeThresholdPixels(gray, dataType);
}

/**
 * 1. ZBar的解析相对较快且容错率高，先用 zbar 解析
 * @param gray
 */
ZXing::Result ImageScheduler::decodeZBar(const cv::Mat &gray, int dataType) {
    LOGE("start zbar gray...")

    ZXing::Result result = zbarDecode(gray);
    if (result.isValid()) {
        logDecode(SCAN_ZBAR, SCAN_TREAT_GRAY);
        return result;
    } else {
        return decodeThresholdPixels(gray, dataType);
    }
}

/**
 * 2. 如果gray化没有解析出来，尝试
 * 提升亮度，处理图片亮度过低时的情况
 * 二值化处理，让二维码更清晰
 * 旋转90度
 *
 * @param gray
 */
ZXing::Result ImageScheduler::decodeThresholdPixels(const cv::Mat &gray, int dataType) {
    LOGE("start ThresholdPixels...")

    cv::Mat mat;
    rotate(gray, mat, cv::ROTATE_180);

    // 提升亮度
    if (m_CameraLight < 80) {
        mat.convertTo(mat, -1, 1.0, 30);
    }

    threshold(mat, mat, 50, 255, cv::THRESH_OTSU);

    ZXing::Result result = zxingDecode(mat, dataType);
    if (result.isValid()) {
        logDecode(SCAN_ZXING, SCAN_TREAT_THRESHOLD);
        return result;
    } else {
        return decodeAdaptivePixels(gray, dataType);
    }
}

/**
 * 3. 降低图片亮度，再次识别图像，处理亮度过高时的情况
 * 逆时针旋转90度，即旋转了270度
 * @param gray
 */
ZXing::Result ImageScheduler::decodeAdaptivePixels(const cv::Mat &gray, int dataType) {
    LOGE("start AdaptivePixels...")

    cv::Mat mat;
    cv::rotate(gray, mat, cv::ROTATE_90_COUNTERCLOCKWISE);

    // 降低图片亮度
    cv::Mat lightMat;
    mat.convertTo(lightMat, -1, 1.0, -60);

    adaptiveThreshold(lightMat, lightMat, 255, cv::ADAPTIVE_THRESH_MEAN_C,
                      cv::THRESH_BINARY, 55, 3);

    ZXing::Result result = zbarDecode(lightMat);
    if (result.isValid()) {
        logDecode(SCAN_ZBAR, SCAN_TREAT_ADAPTIVE);
        return result;
    } else {
        return decodeNegative(gray, dataType);
    }
}

/**
 * 4. 处理反色图片
 * @param gray
 */
ZXing::Result ImageScheduler::decodeNegative(const cv::Mat &gray, int dataType) {
    cv::Mat negativeMat;
    bitwise_not(gray, negativeMat);

    ZXing::Result result = zbarDecode(negativeMat);
    if (result.isValid()) {
        logDecode(SCAN_ZBAR, SCAN_TREAT_NEGATIVE);
        return result;
    } else {
        return ZXing::Result(ZXing::DecodeStatus::NotFound);
    }
}

ZXing::Result ImageScheduler::zxingDecode(const cv::Mat &mat, int dataType) {
    std::shared_ptr<ZXing::BinaryBitmap> binImage;
    LOGE("zxing decode, data type = %d", dataType)
    if (dataType == DATA_TYPE_BYTE) {
        binImage = BinaryBitmapFromBytesC1(mat.data, 0, 0, mat.cols, mat.rows);
    } else {
        binImage = BinaryBitmapFromBytesC4(mat.data, 0, 0, mat.cols, mat.rows);
    }
    ZXing::Result result = reader->read(*binImage);
    if (result.isValid()) {
        LOGE("zxing decode success, result data = %s", result.text().c_str())
    }
    return result;
}

ZXing::Result ImageScheduler::zbarDecode(const cv::Mat &gray) {
    auto width = static_cast<unsigned int>(gray.cols);
    auto height = static_cast<unsigned int>(gray.rows);
    const void *raw = gray.data;
    return zbarDecode(raw, width, height);
}

ZXing::Result ImageScheduler::zbarDecode(const void *raw, unsigned int width, unsigned int height) {
    zbar::Image image(width, height, "Y800", raw, width * height);

    // 检测到二维码
    if (zbarScanner->scan(image) > 0) {
        zbar::Image::SymbolIterator symbol = image.symbol_begin();
        LOGE("zbar decode success, result data = %s", symbol->get_data().c_str())

        ZXing::Result result(ZXing::DecodeStatus::NoError);
        if (symbol->get_type() == zbar::ZBAR_QRCODE) {
            result.setFormat(ZXing::BarcodeFormat::QR_CODE);
        }
        result.setText(ANSIToUnicode(symbol->get_data()));
        image.set_data(nullptr, 0);
        return result;
    }
    image.set_data(nullptr, 0);
    return ZXing::Result(ZXing::DecodeStatus::NotFound);
}

double ImageScheduler::analysisBrightness(const cv::Mat &gray) {
    LOGE("start analysisBrightness...")
    // 平均亮度
    m_CameraLight = mean(gray).val[0];
    LOGE("平均亮度 %lf", m_CameraLight)
    return m_CameraLight;
}

void ImageScheduler::logDecode(int scanType, int treatType) {
    std::string scanName = scanType == SCAN_ZXING ? "zxing" : "zbar";
    LOGE("%s decode success, treat type = %d", scanName.c_str(), treatType)
}

void ImageScheduler::saveMat(const cv::Mat &mat, const std::string &fileName) {
    std::string filePath =
            "/storage/emulated/0/Android/data/me.devilsen.czxing/cache/" + fileName +
            ".jpg";
//    cv::Mat resultMat(mat.rows, mat.cols, CV_8UC1, mat.data);
    bool saveResult = imwrite(filePath, mat);
    if (saveResult) {
        LOGE("save result success filePath = %s", filePath.c_str())
    } else {
        LOGE("save result fail")
    }
}

void ImageScheduler::saveIncreaseMat(const cv::Mat &mat) {
    std::string fileName = std::to_string(m_FileIndex);
    saveMat(mat, fileName);
    m_FileIndex++;
}