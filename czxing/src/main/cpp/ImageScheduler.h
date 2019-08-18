//
// Created by Devilsen on 2019-08-09.
//

#ifndef CZXING_IMAGESCHEDULER_H
#define CZXING_IMAGESCHEDULER_H


#include <jni.h>
#include <opencv2/core/mat.hpp>
#include <src/MultiFormatReader.h>
#include <src/BinaryBitmap.h>
#include "Result.h"
#include "JavaCallHelper.h"

using namespace cv;
using namespace ZXing;

typedef struct FrameData {
    jbyte *bytes;
    int left;
    int top;
    int cropWidth;
    int cropHeight;
    int rowWidth;
    int rowHeight;
} FrameData;

class ImageScheduler {
public:
    ImageScheduler(JNIEnv *env, MultiFormatReader *_reader, JavaCallHelper *javaCallHelper);

    ~ImageScheduler();

    void
    process(jbyte *bytes, int left, int top, int width, int height, int rowWidth,
            int rowHeight);

    void readyMat();

    void decodeGrayPixels(Mat gray);

    void decodeThresholdPixels(Mat gray);

    void decodeAdaptivePixels(Mat gray);

    FrameData frameData;

    Result readBitmap(jobject bitmap, int left, int top, int width,int height);

private:
    JNIEnv *env;
    MultiFormatReader *reader;
    JavaCallHelper *javaCallHelper;
    bool isProcessing = false;

    pthread_t pretreatmentThread;

    Result decodePixels(Mat mat);

    Result *analyzeResult();

};

#endif //CZXING_IMAGESCHEDULER_H
