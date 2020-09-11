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

    void decodeNegative(const Mat &gray);

    Result readBitmap(JNIEnv *env, jobject bitmap, int left, int top, int width, int height);

    void isDecodeQrCode(bool decodeQrCode);

    void setOpenCVDetectValue(int value);

    MultiFormatReader *reader;

private:
    JNIEnv *env;
    JavaCallHelper *javaCallHelper;
    QRCodeRecognizer *qrCodeRecognizer;
    ImageScanner *zbarScanner;

    SafeQueue<FrameData> frameQueue;
    std::atomic<bool> isProcessing{};
    std::atomic<bool> stopProcessing{};
    double cameraLight{};
    int scanIndex;
    bool decodeQr;
    // openCV 探测强度，[0-10]，强度越低，验证越严格，越不容易放大
    int openCVDetectValue = 10;

    pthread_t prepareThread{};

    void recognizerQrCode(const Mat &mat);

    bool zxingDecode(const Mat &mat);

    bool zbarDecode(const Mat &mat);

    bool zbarDecode(const void *raw, unsigned int width, unsigned int height);

    static void logDecode(int scanType, int treatType, int index);

    bool analysisBrightness(const Mat &gray);
};

#endif //CZXING_IMAGESCHEDULER_H
