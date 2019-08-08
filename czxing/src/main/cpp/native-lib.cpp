#include <jni.h>
#include <string>
#include "JNIUtils.h"
#include "MultiFormatReader.h"
#include "DecodeHints.h"
#include "Result.h"
#include "QRCodeRecognizer.h"
#include "opencv2/opencv.hpp"
#include "ImageUtil.h"
#include <vector>
#include <opencv2/imgproc/types_c.h>
#include "MultiFormatWriter.h"
#include "BitMatrix.h"

static std::vector<ZXing::BarcodeFormat> GetFormats(JNIEnv *env, jintArray formats) {
    std::vector<ZXing::BarcodeFormat> result;
    jsize len = env->GetArrayLength(formats);
    if (len > 0) {
        std::vector<jint> elems(len);
        env->GetIntArrayRegion(formats, 0, elems.size(), elems.data());
        result.resize(len);
        for (jsize i = 0; i < len; ++i) {
            result[i] = ZXing::BarcodeFormat(elems[i]);
        }
    }
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_me_devilsen_czxing_BarcodeReader_createInstance(JNIEnv *env, jclass type, jintArray formats_) {
    try {
        ZXing::DecodeHints hints;
        if (formats_ != nullptr) {
            hints.setPossibleFormats(GetFormats(env, formats_));
        }
        return reinterpret_cast<jlong>(new ZXing::MultiFormatReader(hints));
    }
    catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    }
    catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }
    return 0;
}
extern "C"
JNIEXPORT void JNICALL
Java_me_devilsen_czxing_BarcodeReader_destroyInstance(JNIEnv *env, jclass type, jlong objPtr) {

    try {
        delete reinterpret_cast<ZXing::MultiFormatReader *>(objPtr);
    }
    catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    }
    catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }
}
extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_BarcodeReader_readBarcode(JNIEnv *env, jclass type, jlong objPtr,
                                                  jobject bitmap, jint left, jint top, jint width,
                                                  jint height, jobjectArray result) {

    try {
        auto reader = reinterpret_cast<ZXing::MultiFormatReader *>(objPtr);
        auto binImage = BinaryBitmapFromJavaBitmap(env, bitmap, left, top, width, height);
        if (!binImage) {
            return -1;
        }
        auto readResult = reader->read(*binImage);
        if (readResult.isValid()) {
            env->SetObjectArrayElement(result, 0, ToJavaString(env, readResult.text()));
            if (!readResult.resultPoints().empty()) {
                env->SetObjectArrayElement(result, 1, ToJavaArray(env, readResult.resultPoints()));
            }
            return static_cast<int>(readResult.format());
        }
    }
    catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    }
    catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }
    return -1;

}

ZXing::Result
decodePixels(JNIEnv *env, ZXing::MultiFormatReader *reader, void *pixels, int width, int height) {
    auto binImage = BinaryBitmapFromBytesC1(env, pixels, 0, 0, width, height);
    return reader->read(*binImage);
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_BarcodeReader_readBarcodeByte(JNIEnv *env, jclass type, jlong objPtr,
                                                      jbyteArray bytes_, jint left, jint top,
                                                      jint cropWidth, jint cropHeight,
                                                      jint rowWidth, jint rowHeight,
                                                      jobjectArray result) {
    jbyte *bytes = env->GetByteArrayElements(bytes_, NULL);
    ImageUtil imageUtil;
    imageUtil.checkSize(&left, &top);

    try {
        Mat src(rowHeight + rowHeight / 2, rowWidth, CV_8UC1, bytes);
//        imwrite("/storage/emulated/0/scan/src.jpg", src);

        cvtColor(src, src, COLOR_YUV2RGBA_NV21);
//        imwrite("/storage/emulated/0/scan/src2.jpg", src);

        if (left != 0) {
            src = src(Rect(left, top, cropWidth, cropHeight));
        }

        rotate(src, src, ROTATE_90_CLOCKWISE);
//        imwrite("/storage/emulated/0/scan/src3.jpg", src);

        Mat gray;
        cvtColor(src, gray, COLOR_RGBA2GRAY);
//        imwrite("/storage/emulated/0/scan/gray.jpg", gray);

        // 降低图片亮度
        Mat lightMat;
        gray.convertTo(lightMat, -1, 1.0, -60);
//        imwrite("/storage/emulated/0/scan/lightMat.jpg", lightMat);

        // 二值化
        Mat thresholdMat;
        threshold(lightMat, thresholdMat, 0, 255, CV_THRESH_OTSU);
//        imwrite("/storage/emulated/0/scan/threshold.jpg", lightMat);

        auto *pixels = static_cast<unsigned char *>(malloc(
                thresholdMat.cols * thresholdMat.rows * sizeof(unsigned char)));

        imageUtil.getPixelsFromMat(thresholdMat, &cropWidth, &cropHeight, pixels);

        // 检查处理结果
//        Mat resultMat(cropHeight, cropWidth, CV_8UC1, pixels);
//        imwrite("/storage/emulated/0/scan/result.jpg", resultMat);

        auto reader = reinterpret_cast<ZXing::MultiFormatReader *>(objPtr);
        auto readResult = decodePixels(env, reader, pixels, cropWidth, cropHeight);

        std::vector<ZXing::ResultPoint> points;
        if (readResult.isValid()) {
            free(pixels);
            env->SetObjectArrayElement(result, 0, ToJavaString(env, readResult.text()));
            return static_cast<int>(readResult.format());
        } else {
            if (readResult.isBlurry()) {
                points = readResult.resultPoints();
            }
            // 翻转一次
            rotate(thresholdMat, thresholdMat, ROTATE_90_CLOCKWISE);
//            imwrite("/storage/emulated/0/scan/result2.jpg", thresholdMat);
            // 获取翻转后的像素
            imageUtil.getPixelsFromMat(thresholdMat, &cropWidth, &cropHeight, pixels);
            // 解析
            readResult = decodePixels(env, reader, pixels, cropWidth, cropHeight);
            if (readResult.isValid()) {
                free(pixels);
                env->SetObjectArrayElement(result, 0, ToJavaString(env, readResult.text()));
                return static_cast<int>(readResult.format());
            } else {
                if (readResult.isBlurry() && readResult.resultPoints().size() > points.size()) {
                    points = readResult.resultPoints();
                }
                // 翻转第二次
                rotate(lightMat, lightMat, ROTATE_180);
                // 处理过暗的图像
                Mat adaptiveThresholdMat;
                adaptiveThreshold(lightMat, adaptiveThresholdMat, 255, ADAPTIVE_THRESH_MEAN_C,
                                  THRESH_BINARY, 55, 3);
//                imwrite("/storage/emulated/0/scan/result3.jpg", adaptiveThresholdMat);
                // 获取二次翻转后的像素
                imageUtil.getPixelsFromMat(adaptiveThresholdMat, &cropWidth, &cropHeight, pixels);
                // 再次解析
                readResult = decodePixels(env, reader, pixels, cropWidth, cropHeight);
                if (readResult.isValid()) {
                    free(pixels);
                    env->SetObjectArrayElement(result, 0, ToJavaString(env, readResult.text()));
                    return static_cast<int>(readResult.format());
                } else {
                    if (readResult.isBlurry() && readResult.resultPoints().size() > points.size()) {
                        points = readResult.resultPoints();
                    }

                    if (points.size() > 1) {
                        points = readResult.resultPoints();
                        env->SetObjectArrayElement(result, 1, ToJavaArray(env, points));
                        return static_cast<int>(readResult.format());
                    }
                    // 启用图片分析，获取图片位置
                    QRCodeRecognizer opencvProcessor;
                    cv::Rect rect;
                    opencvProcessor.processData(lightMat, cropWidth, cropHeight, &rect);
                    free(pixels);
                    if (!rect.empty()) {
                        env->SetObjectArrayElement(result, 2, reactToJavaArray(env, rect));
                        return 1;
                    }
                }
            }
        }
    }
    catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    }
    catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }
    env->ReleaseByteArrayElements(bytes_, bytes, 0);

    return -1;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_devilsen_czxing_BarcodeReader_analysisBrightnessNative(JNIEnv *env, jclass type,
                                                               jbyteArray bytes_, jint width,
                                                               jint height) {
    jbyte *bytes = env->GetByteArrayElements(bytes_, NULL);

    bool isDark = AnalysisBrightness(env, bytes, width, height);
    env->ReleaseByteArrayElements(bytes_, bytes, 0);

    return isDark ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_BarcodeWriter_writeCode(JNIEnv *env, jclass type, jstring content_,
                                                jint width, jint height, jint color,
                                                jstring format_, jobjectArray result) {
    const char *content = env->GetStringUTFChars(content_, 0);
    const char *format = env->GetStringUTFChars(format_, 0);
    try {
        std::wstring wContent;
        wContent = ANSIToUnicode(content);

        ZXing::MultiFormatWriter writer(ZXing::BarcodeFormatFromString(format));
        ZXing::BitMatrix bitMatrix = writer.encode(wContent, width, height);

        if (bitMatrix.empty()) {
            return -1;
        }

        int size = width * height;
        jintArray pixels = env->NewIntArray(size);
        int black = color;
        int white = 0xffffffff;
        int index = 0;
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                int pix = bitMatrix.get(j, i) ? black : white;
                env->SetIntArrayRegion(pixels, index, 1, &pix);
                index++;
            }
        }
        env->SetObjectArrayElement(result, 0, pixels);
        env->ReleaseStringUTFChars(format_, format);
        env->ReleaseStringUTFChars(content_, content);
    }
    catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    }
    catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }
    return 0;
}