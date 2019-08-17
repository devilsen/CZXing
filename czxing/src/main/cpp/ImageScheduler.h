//
// Created by Devilsen on 2019-08-09.
//

#ifndef CZXING_IMAGESCHEDULER_H
#define CZXING_IMAGESCHEDULER_H


#include <jni.h>
#include <opencv2/core/mat.hpp>
#include <src/MultiFormatReader.h>
#include "Result.h"
#include "JavaCallHelper.h"

using namespace cv;
using namespace ZXing;

class ImageScheduler {
public:
    ImageScheduler(JNIEnv *env, MultiFormatReader *_reader, JavaCallHelper *javaCallHelper);

    ~ImageScheduler();

    Result *
    process(jbyte *bytes, int left, int top, int width, int height, int rowWidth,
            int rowHeight);

    void decodeGrayPixels();

    void decodeThresholdPixels();

    void decodeAdaptivePixels();

private:
    JNIEnv *env;
    MultiFormatReader *reader;
    JavaCallHelper *javaCallHelper;

    pthread_t pretreatmentThread;
    pthread_t grayThread;
    pthread_t thresholdThread;
    pthread_t adaptiveThread;

    Mat pretreatmentMat;

    Result *grayResult;
    Result *thresholdResult;
    Result *adaptiveResult;

    void pretreatment(jbyte *bytes, int left, int top, int width, int height, int rowWidth,
                      int rowHeight);

    void processGray();

    void processThreshold();

    void processAdaptive();

    Result decodePixels(Mat mat);

    Result *analyzeResult();

};

#endif //CZXING_IMAGESCHEDULER_H
