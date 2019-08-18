//
// Created by Devilsen on 2019-08-09.
//

#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/types_c.h>
#include <src/BinaryBitmap.h>
#include "ImageScheduler.h"
#include "JNIUtils.h"

ImageScheduler::ImageScheduler(JNIEnv *env, MultiFormatReader *_reader,
                               JavaCallHelper *javaCallHelper) {
    this->env = env;
    this->reader = _reader;
    this->javaCallHelper = javaCallHelper;
    qrCodeRecognizer = new QRCodeRecognizer();
}

ImageScheduler::~ImageScheduler() {
    DELETE(env);
    DELETE(reader);
    DELETE(javaCallHelper);
    DELETE(qrCodeRecognizer);

    delete &frameData;
}

void *pretreatmentMethod(void *arg) {
    auto scheduler = static_cast<ImageScheduler *>(arg);
    scheduler->readyMat();
    return nullptr;
}

void
ImageScheduler::process(jbyte *bytes, int left, int top, int cropWidth, int cropHeight,
                        int rowWidth,
                        int rowHeight) {
    if (isProcessing) {
        return;
    }
    isProcessing = true;

    LOGE("process begin...");

    frameData.bytes = bytes;
    frameData.left = left;
    frameData.top = top;
    frameData.cropWidth = cropWidth;
    frameData.cropHeight = cropHeight;
    frameData.rowWidth = rowWidth;
    frameData.rowHeight = rowHeight;

    pthread_create(&pretreatmentThread, nullptr, pretreatmentMethod, this);
}

void ImageScheduler::readyMat() {
    // 分析亮度，如果亮度过低，不进行处理
    analysisBrightness(frameData);
    if (cameraLight < 40) {
        isProcessing = false;
        return;
    }

    Mat src(frameData.rowHeight + frameData.rowHeight / 2,
            frameData.rowWidth, CV_8UC1,
            frameData.bytes);

    cvtColor(src, src, COLOR_YUV2RGBA_NV21);

    if (frameData.left != 0) {
        src = src(Rect(frameData.left, frameData.top, frameData.cropWidth, frameData.cropHeight));
    }

    Mat gray;
    cvtColor(src, gray, COLOR_RGBA2GRAY);

    decodeGrayPixels(gray);
}

void ImageScheduler::decodeGrayPixels(Mat gray) {
    Mat mat;
    rotate(gray, mat, ROTATE_90_CLOCKWISE);

    Result result = decodePixels(mat);

    if (result.isValid()) {
        javaCallHelper->onResult(result);
        isProcessing = false;
    } else {
        decodeThresholdPixels(gray);
    }
}

void ImageScheduler::decodeThresholdPixels(Mat gray) {
    Mat mat;
    rotate(gray, mat, ROTATE_180);

    // 提升亮度
    if (cameraLight < 90) {
        mat.convertTo(mat, -1, 1.0, 30);
    }

    threshold(mat, mat, 0, 255, CV_THRESH_OTSU);

    Result result = decodePixels(mat);
    if (result.isValid()) {
        javaCallHelper->onResult(result);
        isProcessing = false;
    } else {
        decodeAdaptivePixels(gray);
    }
}

void ImageScheduler::decodeAdaptivePixels(Mat gray) {
    Mat mat;
    rotate(gray, mat, ROTATE_90_COUNTERCLOCKWISE);

    // 降低图片亮度
    Mat lightMat;
    mat.convertTo(lightMat, -1, 1.0, -60);

    adaptiveThreshold(lightMat, lightMat, 255, ADAPTIVE_THRESH_MEAN_C,
                      THRESH_BINARY, 55, 3);

    Result result = decodePixels(lightMat);
    if (result.isValid()) {
        javaCallHelper->onResult(result);
        isProcessing = false;
    } else {
        recognizerQrCode(gray);
    }
}

void ImageScheduler::recognizerQrCode(Mat mat) {
    cv::Rect rect;
    qrCodeRecognizer->processData(mat, &rect);
    if (rect.empty()) {
        isProcessing = false;
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
    isProcessing = false;
}

Result ImageScheduler::decodePixels(Mat mat) {
    try {
        int width = mat.cols;
        int height = mat.rows;

        auto *pixels = new unsigned char[height * width];

        int index = 0;
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                pixels[index++] = mat.at<unsigned char>(i, j);
            }
        }

//    Mat resultMat(height, width, CV_8UC1, pixels);
//    imwrite("/storage/emulated/0/scan/result.jpg", resultMat);

        auto binImage = BinaryBitmapFromBytesC1(pixels, 0, 0, width, height);
        Result result = reader->read(*binImage);

        delete[]pixels;

        return result;
    } catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    }
    catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }

    return Result(DecodeStatus::NotFound);
}

bool ImageScheduler::analysisBrightness(const FrameData frameData) {
    // 像素点的总亮度
    unsigned long pixelLightCount = 0L;

    // 采集步长，因为没有必要每个像素点都采集，可以跨一段采集一个，减少计算负担，必须大于等于1。
    int step = 8;
    // 仅分析ScanView中的数据
    int start = frameData.top * frameData.rowWidth;
    // 像素点的总数
    int pixelCount = frameData.rowWidth * frameData.cropHeight;
    for (int i = start; i < pixelCount; i += step) {
        // 如果直接加是不行的，因为 data[i] 记录的是色值并不是数值，byte 的范围是 +127 到 —128，
        pixelLightCount += frameData.bytes[i] & 0xffL;
    }

    // 平均亮度
    cameraLight = pixelLightCount / (pixelCount / step);
//    LOGE("平均亮度 %ld", cameraLight);
    // 判断在时间范围 AMBIENT_BRIGHTNESS_WAIT_SCAN_TIME * lightSize 内是不是亮度过暗
    bool isDark = cameraLight < 50;
    javaCallHelper->onBrightness(isDark);

    return isDark;
}

Result *ImageScheduler::analyzeResult() {
    Result *result = nullptr;

    return result;
}

Result ImageScheduler::readBitmap(jobject bitmap, int left, int top, int width, int height) {
    auto binImage = BinaryBitmapFromJavaBitmap(env, bitmap, left, top, width, height);
    if (!binImage) {
        LOGE("create binary bitmap fail");
        return Result(DecodeStatus::NotFound);
    }
    return reader->read(*binImage);
}
