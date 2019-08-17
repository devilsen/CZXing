//
// Created by Devilsen on 2019-08-09.
//

#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/types_c.h>
#include "ImageScheduler.h"
#include "JNIUtils.h"

ImageScheduler::ImageScheduler(JNIEnv *env, MultiFormatReader *_reader,
                               JavaCallHelper *javaCallHelper) {
    this->env = env;
    this->reader = _reader;
    this->javaCallHelper = javaCallHelper;
}

ImageScheduler::~ImageScheduler() {
    DELETE(env);
    DELETE(reader);
    DELETE(javaCallHelper);

    cvDet(&pretreatmentMat);

    DELETE(grayResult);
    DELETE(thresholdResult);
    DELETE(adaptiveResult);
}

void
ImageScheduler::pretreatment(jbyte *bytes, int left, int top, int cropWidth, int cropHeight,
                             int rowWidth,
                             int rowHeight) {
    Mat src(rowHeight + rowHeight / 2, rowWidth, CV_8UC1, bytes);

    cvtColor(src, src, COLOR_YUV2RGBA_NV21);

    if (left != 0) {
        src = src(Rect(left, top, cropWidth, cropHeight));
    }

    rotate(src, src, ROTATE_90_CLOCKWISE);

    Mat gray;
    cvtColor(src, gray, COLOR_RGBA2GRAY);

    pretreatmentMat = gray;
}

Result *
ImageScheduler::process(jbyte *bytes, int left, int top, int cropWidth, int cropHeight,
                        int rowWidth,
                        int rowHeight) {

    pretreatment(bytes, left, top, cropWidth, cropHeight, rowWidth, rowHeight);

//    decodeGrayPixels();
    processGray();
    processThreshold();
    processAdaptive();

//    return analyzeResult();
    return nullptr;
}

void *threadProcessGray(void *args) {
    auto *scheduler = static_cast<ImageScheduler *>(args);
    scheduler->decodeGrayPixels();
    return nullptr;
}

void *threadProcessThreshold(void *args) {
    auto *scheduler = static_cast<ImageScheduler *>(args);
    scheduler->decodeThresholdPixels();
    return nullptr;
}

void *threadProcessAdaptive(void *args) {
    auto *scheduler = static_cast<ImageScheduler *>(args);
    scheduler->decodeAdaptivePixels();
    return nullptr;
}

void ImageScheduler::processGray() {
    pthread_create(&grayThread, nullptr, threadProcessGray, this);
}

void ImageScheduler::processThreshold() {
    pthread_create(&thresholdThread, nullptr, threadProcessThreshold, this);
}

void ImageScheduler::processAdaptive() {
    pthread_create(&adaptiveThread, nullptr, threadProcessAdaptive, this);
}

void ImageScheduler::decodeGrayPixels() {
    Result result = decodePixels(pretreatmentMat);
    javaCallHelper->onResult(result);
}

void ImageScheduler::decodeThresholdPixels() {
    Mat mat;
    rotate(pretreatmentMat, mat, ROTATE_90_CLOCKWISE);
    threshold(mat, mat, 0, 255, CV_THRESH_OTSU);

    Result result = decodePixels(mat);
    javaCallHelper->onResult(result);
}

void ImageScheduler::decodeAdaptivePixels() {
    Mat mat;
    rotate(pretreatmentMat, mat, ROTATE_180);

    // 降低图片亮度
    Mat lightMat;
    mat.convertTo(lightMat, -1, 1.0, -60);

    adaptiveThreshold(lightMat, lightMat, 255, ADAPTIVE_THRESH_MEAN_C,
                      THRESH_BINARY, 55, 3);

    Result result = decodePixels(lightMat);
    javaCallHelper->onResult(result);
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

        delete pixels;

        return result;
    } catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    }
    catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }

    return Result(DecodeStatus::NotFound);
}

Result *ImageScheduler::analyzeResult() {
    Result *result = nullptr;

    if (grayResult != nullptr) {
        if (grayResult->isValid()) {
            return grayResult;
        } else if (grayResult->isBlurry()) {
            result = grayResult;
        }
    }

    if (thresholdResult != nullptr) {
        if (thresholdResult->isValid()) {
            return thresholdResult;
        } else if (thresholdResult->isBlurry() && thresholdResult->resultPoints().size() > 2) {
            result = thresholdResult;
        }
    }

    if (adaptiveResult != nullptr) {
        if (adaptiveResult->isValid()) {
            return adaptiveResult;
        } else if (adaptiveResult->isBlurry() && adaptiveResult->resultPoints().size() > 2) {
            result = adaptiveResult;
        }
    }

    return result;
}