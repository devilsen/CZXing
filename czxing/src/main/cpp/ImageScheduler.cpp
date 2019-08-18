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
}

ImageScheduler::~ImageScheduler() {
    DELETE(env);
    DELETE(reader);
    DELETE(javaCallHelper);

    delete &frameData;
}

void *pretreatmentMethod(void *arg) {
    auto scheduler = static_cast<ImageScheduler *>(arg);
    scheduler->readyMat();
    return nullptr;
}

void ImageScheduler::readyMat() {
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

void
ImageScheduler::process(jbyte *bytes, int left, int top, int cropWidth, int cropHeight,
                        int rowWidth,
                        int rowHeight) {
    if (isProcessing) {
        return;
    }
    isProcessing = true;

    frameData.bytes = bytes;
    frameData.left = left;
    frameData.top = top;
    frameData.cropWidth = cropWidth;
    frameData.cropHeight = cropHeight;
    frameData.rowWidth = rowWidth;
    frameData.rowHeight = rowHeight;

    pthread_create(&pretreatmentThread, nullptr, pretreatmentMethod, this);
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
