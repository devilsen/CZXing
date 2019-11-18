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
#include "safe_queue.h"
#include "zbar/zbar.h"

using namespace cv;
using namespace ZXing;
using namespace zbar;

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

    void prepare();

    void start();

    void stop();

    void preTreatMat(const FrameData &frameData);

    void decodeGrayPixels(const Mat &gray);

    void decodeZBar(const Mat &gray);

    void decodeThresholdPixels(const Mat &gray);

    void decodeAdaptivePixels(const Mat &gray);

    Result readBitmap(jobject bitmap, int left, int top, int width, int height);

    void isDecodeQrCode(bool decodeQrCode);

    MultiFormatReader *reader;

private:
    JNIEnv *env;
    JavaCallHelper *javaCallHelper;
    std::atomic<bool> isProcessing{};
    std::atomic<bool> stopProcessing{};
    double cameraLight{};
    QRCodeRecognizer *qrCodeRecognizer;
    SafeQueue<FrameData> frameQueue;
    int scanIndex;
    bool decodeQr;

    pthread_t prepareThread{};

    Result decodePixels(const Mat &mat);

    void recognizerQrCode(const Mat &mat);

    Result *analyzeResult();

    bool analysisBrightness(const Mat &gray);

};

#endif //CZXING_IMAGESCHEDULER_H
