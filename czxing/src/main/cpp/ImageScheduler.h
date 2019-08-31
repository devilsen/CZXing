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
#include "QRCodeRecognizer.h"

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

    void decodeGrayPixels(const Mat& gray);

    void decodeThresholdPixels(const Mat& gray);

    void decodeAdaptivePixels(const Mat& gray);

    FrameData frameData{};

    Result readBitmap(jobject bitmap, int left, int top, int width,int height);

private:
    JNIEnv *env;
    MultiFormatReader *reader;
    JavaCallHelper *javaCallHelper;
    bool isProcessing = false;
    long cameraLight{};
    QRCodeRecognizer *qrCodeRecognizer;

    pthread_t pretreatmentThread{};

    Result decodePixels(Mat mat);

    void recognizerQrCode(const Mat& mat);

    Result *analyzeResult();

    bool  analysisBrightness(const FrameData frameData);

};

#endif //CZXING_IMAGESCHEDULER_H
