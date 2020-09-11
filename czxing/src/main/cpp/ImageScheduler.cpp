//
// Created by Devilsen on 2019-08-09.
//

#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/types_c.h>
#include <src/BinaryBitmap.h>
#include "ImageScheduler.h"
#include "JNIUtils.h"
#include <unistd.h>

int DEFAULT_MIN_LIGHT = 70;

int SCAN_ZXING = 0;
int SCAN_ZBAR = 1;

int SCAN_TREAT_GRAY = 0;
int SCAN_TREAT_THRESHOLD = 1;
int SCAN_TREAT_ADAPTIVE = 2;
int SCAN_TREAT_NEGATIVE = 3;
int SCAN_TREAT_CUSTOMIZE = 4;

ImageScheduler::ImageScheduler(JNIEnv *env, MultiFormatReader *_reader,
                               JavaCallHelper *javaCallHelper) {
    this->env = env;
    this->reader = _reader;
    this->javaCallHelper = javaCallHelper;
    qrCodeRecognizer = new QRCodeRecognizer();
    zbarScanner = new ImageScanner();
    zbarScanner->set_config(ZBAR_QRCODE, ZBAR_CFG_ENABLE, 1);

    decodeQr = true;
    stopProcessing.store(false);
    isProcessing.store(false);
}

ImageScheduler::~ImageScheduler() {
    DELETE(env)
    DELETE(reader)
    DELETE(javaCallHelper)
    DELETE(qrCodeRecognizer)
    frameQueue.clear();
    delete zbarScanner;
    delete &isProcessing;
    delete &stopProcessing;
    delete &cameraLight;
    delete &prepareThread;
    scanIndex = 0;
    decodeQr = false;
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
        // 在V7环境下会崩溃
//        usleep(50000);
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
    LOGE("frame data size : %d", frameQueue.size());
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

/**
 * 1.1
 * 直接解析gray后的mat
 * 顺时针旋转90度图片，得到正常的图片（Android的后置摄像头获取的格式是横着的，需要旋转90度）
 * @param gray
 */
void ImageScheduler::decodeGrayPixels(const Mat &gray) {
    LOGE("start GrayPixels...");

    Mat mat;
    rotate(gray, mat, ROTATE_90_CLOCKWISE);

    bool result = zxingDecode(mat);
    if (result) {
        logDecode(SCAN_ZXING, SCAN_TREAT_GRAY, scanIndex);
    } else {
        decodeThresholdPixels(gray);
    }
}

/**
 * 1.2
 * 如果gray化没有解析出来，尝试提升亮度，处理图片亮度过低时的情况
 * 并进行二值化处理，让二维码更清晰
 * 同时旋转180度
 * @param gray
 */
void ImageScheduler::decodeThresholdPixels(const Mat &gray) {
    LOGE("start ThresholdPixels...");

    Mat mat;
    rotate(gray, mat, ROTATE_180);

    // 提升亮度
    if (cameraLight < 80) {
        mat.convertTo(mat, -1, 1.0, 30);
    }

    threshold(mat, mat, 50, 255, CV_THRESH_OTSU);

    bool result = zxingDecode(mat);
    if (result) {
        logDecode(SCAN_ZXING, SCAN_TREAT_THRESHOLD, scanIndex);
    } else {
        recognizerQrCode(gray);
    }
}

/**
 * 2.1
 * 使用ZBar用来补充ZXing对于某些情况（比如，倾斜角度过大）的不足
 * @param gray
 */
void ImageScheduler::decodeZBar(const Mat &gray) {
    LOGE("start zbar gray...");

    bool result = zbarDecode(gray);
    if (result) {
        logDecode(SCAN_ZBAR, SCAN_TREAT_GRAY, scanIndex);
    } else {
        decodeAdaptivePixels(gray);
    }
}

/**
 * 2.2 降低图片亮度，再次识别图像，处理亮度过高时的情况
 * 逆时针旋转90度，即旋转了270度
 * @param gray
 */
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

    bool result = zbarDecode(lightMat);
    if (result) {
        logDecode(SCAN_ZBAR, SCAN_TREAT_ADAPTIVE, scanIndex);
    } else {
        decodeNegative(gray);
    }
}

/**
 * 2.3 处理反色图片
 * @param gray
 */
void ImageScheduler::decodeNegative(const Mat &gray) {
    Mat negativeMat;
    bitwise_not(gray, negativeMat);

    bool result = zbarDecode(negativeMat);
    if (result) {
        logDecode(SCAN_ZBAR, SCAN_TREAT_NEGATIVE, scanIndex);
    } else {
        recognizerQrCode(gray);
    }
}

/**
 * 3.0
 * 不能使用内置的解析出来，使用自定的图片解析策略
 */
void ImageScheduler::recognizerQrCode(const Mat &mat) {
    // 强度为 0，外层不需要 openCV 介入
    if (openCVDetectValue == 0) {
        return;
    }
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
    LOGE("start to recognizerQrCode..., scanIndex = %d ", scanIndex);

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

    javaCallHelper->onResult(result, SCAN_TREAT_CUSTOMIZE);

    LOGE("end recognizerQrCode..., scanIndex = %d height = %d width = %d", scanIndex, rect.height,
         rect.width);
}

bool ImageScheduler::zxingDecode(const Mat &mat) {
    auto binImage = BinaryBitmapFromBytesC1(mat.data, 0, 0, mat.cols, mat.rows);
    Result result = reader->read(*binImage);
    if (result.isValid()) {
        LOGE("zxing decode success, result data = %s", result.text().c_str());
        javaCallHelper->onResult(result, SCAN_ZXING);
        return true;
    }
    return false;
}

bool ImageScheduler::zbarDecode(const Mat &gray) {
    auto width = static_cast<unsigned int>(gray.cols);
    auto height = static_cast<unsigned int>(gray.rows);
    const void *raw = gray.data;
    return zbarDecode(raw, width, height);
}

bool ImageScheduler::zbarDecode(const void *raw, unsigned int width, unsigned int height) {
    Image image(width, height, "Y800", raw, width * height);

    // 检测到二维码
    if (zbarScanner->scan(image) > 0) {
        Image::SymbolIterator symbol = image.symbol_begin();
        LOGE("zbar decode success, result data = %s", symbol->get_data().c_str());

        if (symbol->get_type() == ZBAR_QRCODE) {
            Result result(DecodeStatus::NoError);
            result.setFormat(BarcodeFormat::QR_CODE);
            result.setText(ANSIToUnicode(symbol->get_data()));
            javaCallHelper->onResult(result, SCAN_ZBAR);
            return true;
        }
    }
    image.set_data(nullptr, 0);
    return false;
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

//void saveMat(){
//        Mat resultMat(height, width, CV_8UC1, pixels);
//        imwrite("/storage/emulated/0/scan/result.jpg", mat);
//}

void ImageScheduler::logDecode(int scanType, int treatType, int index) {
    String scanName = scanType == SCAN_ZXING ? "zxing" : "zbar";
    LOGE("%s decode success, treat type = %d, scan index = %d",
         scanName.c_str(), treatType, index);
}

void ImageScheduler::isDecodeQrCode(bool decodeQrCode) {
    this->decodeQr = decodeQrCode;
}

void ImageScheduler::setOpenCVDetectValue(int value) {
    this->openCVDetectValue = value;
}

Result
ImageScheduler::readBitmap(JNIEnv *env, jobject bitmap, int left, int top, int width, int height) {

    Mat src;
    BitmapToMat(env, bitmap, src);

    Mat gray;
    cvtColor(src, gray, COLOR_RGBA2GRAY);

    auto gWidth = static_cast<unsigned int>(gray.cols);
    auto gHeight = static_cast<unsigned int>(gray.rows);
    const void *raw = gray.data;
    Image image(gWidth, gHeight, "Y800", raw, gWidth * gHeight);
    LOGE("zbar Code cols = %d row = %d", gray.cols, gray.rows);

    if (zbarScanner->scan(image) > 0) {
        Image::SymbolIterator symbol = image.symbol_begin();
        LOGE("zbar Code Data = %s", symbol->get_data().c_str());
        if (symbol->get_type() == zbar_symbol_type_e::ZBAR_QRCODE) {
            Result resultBar(DecodeStatus::NoError);
            resultBar.setFormat(BarcodeFormat::QR_CODE);
            resultBar.setText(ANSIToUnicode(symbol->get_data()));
            image.set_data(nullptr, 0);
            LOGE("zbar decode success");
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
    LOGE("zxing decode success");

    return reader->read(*binImage);
}