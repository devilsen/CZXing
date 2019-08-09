//
// Created by Devilsen on 2019-08-09.
//

#ifndef CZXING_IMAGESCHEDULER_H
#define CZXING_IMAGESCHEDULER_H


#include <jni.h>
#include <opencv2/core/mat.hpp>
#include <src/MultiFormatReader.h>
#include "Result.h"

using namespace cv;
using namespace ZXing;

class ImageScheduler {
public:
    ImageScheduler(JNIEnv *env, MultiFormatReader *_reader);

    ~ImageScheduler();

    Result *
    process(JNIEnv *env, jbyte *bytes, int left, int top, int width, int height, int rowWidth,
            int rowHeight);

    void decodeGrayPixels();

    void decodeThresholdPixels();

    void decodeAdaptivePixels();

private:
    JNIEnv *env;
    MultiFormatReader *reader;

    pthread_t grayThread;
    pthread_t thresholdThread;
    pthread_t adaptiveThread;

    Mat *grayMat;
    Mat *thresholdMat;
    Mat *adaptiveMat;

    Result *grayResult;
    Result *thresholdResult;
    Result *adaptiveResult;

    Mat pretreatment(jbyte *bytes, int left, int top, int width, int height, int rowWidth,
                     int rowHeight);

    void processGray(Mat gray);

    void processThreshold(Mat gray);

    void processAdaptive(Mat gray);

    void decodePixels(Mat *mat, Result *result);

    Result *analyzeResult();

    void getPixelsFromMat(Mat mat, int width, int height, unsigned char *pixels);

};


#endif //CZXING_IMAGESCHEDULER_H
