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

    DELETE(grayMat);
    DELETE(thresholdMat);
    DELETE(adaptiveMat);

    DELETE(grayResult);
    DELETE(thresholdResult);
    DELETE(adaptiveResult);
}

Mat
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

    // 降低图片亮度
    Mat lightMat;
    gray.convertTo(lightMat, -1, 1.0, -60);

    return lightMat;
}

Result *
ImageScheduler::process(JNIEnv *env, jbyte *bytes, int left, int top, int cropWidth, int cropHeight,
                        int rowWidth,
                        int rowHeight) {

    Mat gray = pretreatment(bytes, left, top, cropWidth, cropHeight, rowWidth, rowHeight);

    processGray(gray);
//        processThreshold(gray);
//        processAdaptive(gray);

    return analyzeResult();
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

void ImageScheduler::processGray(Mat gray) {
    grayMat = &gray;
    pthread_create(&grayThread, nullptr, threadProcessGray, this);
    pthread_join(grayThread, nullptr);
}

void ImageScheduler::processThreshold(Mat gray) {
    Mat mat;
    rotate(gray, mat, ROTATE_90_CLOCKWISE);
    threshold(mat, mat, 0, 255, CV_THRESH_OTSU);

    thresholdMat = &mat;
    pthread_create(&thresholdThread, nullptr, threadProcessThreshold, &mat);
    pthread_join(thresholdThread, nullptr);
}

void ImageScheduler::processAdaptive(Mat gray) {
    Mat mat;
    rotate(gray, mat, ROTATE_180);
    adaptiveThreshold(mat, mat, 255, ADAPTIVE_THRESH_MEAN_C,
                      THRESH_BINARY, 55, 3);

    adaptiveMat = &mat;
    pthread_create(&adaptiveThread, nullptr, threadProcessAdaptive, &mat);
    pthread_join(adaptiveThread, nullptr);
}

void ImageScheduler::getPixelsFromMat(Mat mat, int width, int height, unsigned char *pixels) {
    int index = 0;
    for (int i = 0; i < height; ++i) {
        for (int j = 0; j < width; ++j) {
            pixels[index++] = mat.at<unsigned char>(i, j);
        }
    }
}

void ImageScheduler::decodePixels(Mat *mat, Result *result) {
    int width = mat->cols;
    int height = mat->rows;

    auto *pixels = static_cast<unsigned char *>(malloc(
            width * height * sizeof(unsigned char)));

    getPixelsFromMat(*mat, width, height, pixels);

//    Mat resultMat(height, width, CV_8UC1, pixels);
//    imwrite("/storage/emulated/0/scan/result.jpg", resultMat);

    try {
        auto binImage = BinaryBitmapFromBytesC1(pixels, 0, 0, width, height);
        free(pixels);
        *result = reader->read(*binImage);

//        javaCallHelper->onResult(result);
    } catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    }
    catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }
}

Result *ImageScheduler::analyzeResult() {
    Result *result = nullptr;

    if (grayResult) {
        if (grayResult->isValid()) {
            return grayResult;
        } else if (grayResult->isBlurry()) {
            result = grayResult;
        }
    }

    if (thresholdResult) {
        if (thresholdResult->isValid()) {
            return thresholdResult;
        } else if (thresholdResult->isBlurry() && thresholdResult->resultPoints().size() > 2) {
            result = thresholdResult;
        }
    }

    if (adaptiveResult) {
        if (adaptiveResult->isValid()) {
            return adaptiveResult;
        } else if (adaptiveResult->isBlurry() && adaptiveResult->resultPoints().size() > 2) {
            result = adaptiveResult;
        }
    }

    return result;
}

void ImageScheduler::decodeGrayPixels() {
    javaCallHelper->onTest();

    decodePixels(grayMat, grayResult);
}

void ImageScheduler::decodeThresholdPixels() {
    decodePixels(thresholdMat, thresholdResult);

}

void ImageScheduler::decodeAdaptivePixels() {
    decodePixels(adaptiveMat, adaptiveResult);

}

