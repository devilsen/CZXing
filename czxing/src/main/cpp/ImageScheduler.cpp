//
// Created by Devilsen on 2019-08-09.
//

#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/types_c.h>
#include <src/BinaryBitmap.h>
#include "ImageScheduler.h"
#include "JNIUtils.h"

#define DEFAULT_MIN_LIGHT 70;

ImageScheduler::ImageScheduler(JNIEnv *env, MultiFormatReader *_reader,
                               JavaCallHelper *javaCallHelper) {
    this->env = env;
    this->reader = _reader;
    this->javaCallHelper = javaCallHelper;
    qrCodeRecognizer = new QRCodeRecognizer();
    decodeQr = true;
    stopProcessing.store(false);
    isProcessing.store(false);
}

ImageScheduler::~ImageScheduler() {
    DELETE(env);
    DELETE(reader);
    DELETE(javaCallHelper);
    DELETE(qrCodeRecognizer);
    frameQueue.clear();
    delete &isProcessing;
    delete &stopProcessing;
    delete &cameraLight;
    delete &prepareThread;
    scanIndex = 0;
    decodeQr = 0;
}

void *prepareMethod(void *arg) {
    auto scheduler = static_cast<ImageScheduler *>(arg);
    scheduler->start();
    return nullptr;
}

void ImageScheduler::prepare() {
    pthread_create(&prepareThread, nullptr, prepareMethod, this);
}

void ImageScheduler::start() {
    stopProcessing.store(false);
    isProcessing.store(false);
    frameQueue.setWork(1);
    scanIndex = 0;

    while (true) {
        if (stopProcessing.load()) {
            break;
        }

        if (isProcessing.load()) {
            continue;
        }

        FrameData frameData;
        int ret = frameQueue.deQueue(frameData);
        if (ret) {
            isProcessing.store(true);
            preTreatMat(frameData);
            isProcessing.store(false);
        }
    }
}

void ImageScheduler::stop() {
    isProcessing.store(false);
    stopProcessing.store(true);
    frameQueue.setWork(0);
    frameQueue.clear();
    scanIndex = 0;
}

void
ImageScheduler::process(jbyte *bytes, int left, int top, int cropWidth, int cropHeight,
                        int rowWidth,
                        int rowHeight) {
    if (isProcessing.load()) {
        return;
    }

    FrameData frameData;
    frameData.left = left;
    frameData.top = top;
    frameData.cropWidth = cropWidth;
    if (top + cropHeight > rowHeight) {
        frameData.cropHeight = rowHeight - top;
    } else {
        frameData.cropHeight = cropHeight;
    }
    if (frameData.cropHeight < frameData.cropWidth) {
        frameData.cropWidth = frameData.cropHeight;
    }
    frameData.rowWidth = rowWidth;
    frameData.rowHeight = rowHeight;
    frameData.bytes = bytes;

    frameQueue.enQueue(frameData);
//    LOGE("frame data size : %d", frameQueue.size());
}

/**
 * 预处理二进制数据
 */
void ImageScheduler::preTreatMat(const FrameData &frameData) {
    try {
        scanIndex++;
        LOGE("start preTreatMat..., scanIndex = %d", scanIndex);

        Mat src(frameData.rowHeight + frameData.rowHeight / 2,
                frameData.rowWidth, CV_8UC1,
                frameData.bytes);

        Mat gray;
        cvtColor(src, gray, COLOR_YUV2GRAY_NV21);

        if (frameData.left != 0) {
            gray = gray(
                    Rect(frameData.left, frameData.top, frameData.cropWidth, frameData.cropHeight));
        }

        // 分析亮度，如果亮度过低，不进行处理
        analysisBrightness(gray);
        if (cameraLight < 30) {
            return;
        }

        // 不需要解析二维码
        if (!decodeQr) {
            decodeGrayPixels(gray);
            return;
        }

        // 正常解析策略 偶数次zxing解析，奇数次zbar解析
        if (scanIndex % 2 == 0) {
            decodeGrayPixels(gray);
        } else {
            decodeZBar(gray);
        }
    } catch (const std::exception &e) {
        LOGE("preTreatMat error...");
    }
}

void ImageScheduler::decodeGrayPixels(const Mat &gray) {
    LOGE("start GrayPixels...");

    Mat mat;
    rotate(gray, mat, ROTATE_90_CLOCKWISE);

    Result result = decodePixels(mat);

    if (result.isValid()) {
        javaCallHelper->onResult(result);
        LOGE("ZXing GrayPixels Success, scanIndex = %d", scanIndex);
    } else {
        decodeThresholdPixels(gray);
    }
}

void ImageScheduler::decodeZBar(const Mat &gray) {
    auto width = static_cast<unsigned int>(gray.cols);
    auto height = static_cast<unsigned int>(gray.rows);

    const void *raw = gray.data;
    Image image(width, height, "Y800", raw, width * height);
    ImageScanner zbarScanner;
    zbarScanner.set_config(ZBAR_QRCODE, ZBAR_CFG_ENABLE, 1);

    // 检测到二维码
    if (zbarScanner.scan(image) > 0) {
        Image::SymbolIterator symbol = image.symbol_begin();
        LOGE("zbar GrayPixels Success  Data = %s scanIndex = %d", symbol->get_data().c_str(),
             scanIndex);

        if (symbol->get_type() == zbar_symbol_type_e::ZBAR_QRCODE) {
            Result result(DecodeStatus::NoError);
            result.setFormat(BarcodeFormat::QR_CODE);
            result.setText(ANSIToUnicode(symbol->get_data()));
            javaCallHelper->onResult(result);
            image.set_data(nullptr, 0);
        }
    } else {
        image.set_data(nullptr, 0);
        decodeAdaptivePixels(gray);
    }
}

void ImageScheduler::decodeThresholdPixels(const Mat &gray) {
    LOGE("start ThresholdPixels...");

    Mat mat;
    rotate(gray, mat, ROTATE_180);

    // 提升亮度
    if (cameraLight < 80) {
        mat.convertTo(mat, -1, 1.0, 30);
    }

    threshold(mat, mat, 50, 255, CV_THRESH_OTSU);

    Result result = decodePixels(mat);
    if (result.isValid()) {
        LOGE("ZXing Threshold Success, scanIndex = %d", scanIndex);
        javaCallHelper->onResult(result);
    } else {
        recognizerQrCode(gray);
    }
}

void ImageScheduler::decodeAdaptivePixels(const Mat &gray) {
    if (scanIndex % 3 != 0) {
        return;
    }
    LOGE("start AdaptivePixels...");

    Mat mat;
    rotate(gray, mat, ROTATE_90_COUNTERCLOCKWISE);

    // 降低图片亮度
    Mat lightMat;
    mat.convertTo(lightMat, -1, 1.0, -60);

    adaptiveThreshold(lightMat, lightMat, 255, ADAPTIVE_THRESH_MEAN_C,
                      THRESH_BINARY, 55, 3);

    Result result = decodePixels(lightMat);
    if (result.isValid()) {
        LOGE("ZXing Adaptive Success, scanIndex = %d", scanIndex);
        javaCallHelper->onResult(result);
    } else {
        recognizerQrCode(gray);
    }
}

void ImageScheduler::recognizerQrCode(const Mat &mat) {
    LOGE("try to recognizerQrCode..., scanIndex = %d ", scanIndex);

    // 不需要解析二维码
    if (!decodeQr) {
        return;
    }
    // 7次没有解析出来，尝试聚焦
    if (scanIndex % 7 == 0) {
        javaCallHelper->onFocus();
    }
    // 只有3的倍数次才去使用OpenCV处理
//    if (scanIndex % 3 != 0) {
//        return;
//    }
    LOGE("start recognizerQrCode...");

    cv::Rect rect;
    qrCodeRecognizer->processData(mat, &rect);
    // 一般认为，长度小于120的一般是误报
    if (rect.empty() || rect.height < 120) {
        return;
    }

    ResultPoint point1(rect.tl().x, rect.tl().y);
    ResultPoint point2(rect.br().x, rect.tl().y);
    ResultPoint point3(rect.tl().x, rect.br().y);

    std::vector<ResultPoint> points;
    points.push_back(point1);
    points.push_back(point2);
    points.push_back(point3);

    Result result(DecodeStatus::NotFound);
    result.setResultPoints(std::move(points));

    javaCallHelper->onResult(result);

    LOGE("end recognizerQrCode..., scanIndex = %d height = %d width = %d", scanIndex, rect.height,
         rect.width);
}

Result ImageScheduler::decodePixels(const Mat &mat) {
    try {
//        Mat resultMat(height, width, CV_8UC1, pixels);
//        imwrite("/storage/emulated/0/scan/result.jpg", resultMat);

        auto binImage = BinaryBitmapFromBytesC1(mat.data, 0, 0, mat.cols, mat.rows);
        return reader->read(*binImage);
    } catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    } catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }

    return Result(DecodeStatus::NotFound);
}

bool ImageScheduler::analysisBrightness(const Mat &gray) {
    LOGE("start analysisBrightness...");

    // 平均亮度
    Scalar scalar = mean(gray);
    cameraLight = scalar.val[0];
    LOGE("平均亮度 %lf", cameraLight);
    // 判断在时间范围 AMBIENT_BRIGHTNESS_WAIT_SCAN_TIME * lightSize 内是不是亮度过暗
    bool isDark = cameraLight < DEFAULT_MIN_LIGHT;
    javaCallHelper->onBrightness(isDark);

    return isDark;
}

Result *ImageScheduler::analyzeResult() {
    Result *result = nullptr;

    return result;
}

void ImageScheduler::isDecodeQrCode(bool decodeQrCode) {
    this->decodeQr = decodeQrCode;
}

Result ImageScheduler::readBitmap(jobject bitmap, int left, int top, int width, int height) {

    Mat src;
    BitmapToMat(env, bitmap, src);

    Mat gray;
    cvtColor(src, gray, COLOR_RGB2GRAY);

    const void *raw = gray.data;
    Image image(gray.cols, gray.rows, "Y800", raw, gray.cols * gray.rows);
    ImageScanner zbarScanner;
    zbarScanner.set_config(ZBAR_QRCODE, ZBAR_CFG_ENABLE, 1);
    if (zbarScanner.scan(image) > 0) {
        Image::SymbolIterator symbol = image.symbol_begin();
        LOGE("zbar Code Data = %s", symbol->get_data().c_str());
        if (symbol->get_type() == zbar_symbol_type_e::ZBAR_QRCODE) {
            Result resultBar(DecodeStatus::NoError);
            resultBar.setFormat(BarcodeFormat::QR_CODE);
            resultBar.setText(ANSIToUnicode(symbol->get_data()));
            image.set_data(nullptr, 0);
            return resultBar;
        }
    } else {
        image.set_data(nullptr, 0);
    }

    auto binImage = BinaryBitmapFromJavaBitmap(env, bitmap, left, top, width, height);
    if (!binImage) {
        LOGE("create binary bitmap fail");
        return Result(DecodeStatus::NotFound);
    }

    return reader->read(*binImage);
}



