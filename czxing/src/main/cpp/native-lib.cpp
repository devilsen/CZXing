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

extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_BarcodeReader_readBarcodeByte(JNIEnv *env, jclass type, jlong objPtr,
                                                      jbyteArray bytes_, jint left, jint top,
                                                      jint cropWidth, jint cropHeight,
                                                      jint rowWidth,
                                                      jobjectArray result) {
    jbyte *bytes = env->GetByteArrayElements(bytes_, NULL);
    ImageUtil imageUtil;
    if (!imageUtil.checkSize(&left, &top)) {
        return -1;
    }

    try {
        auto reader = reinterpret_cast<ZXing::MultiFormatReader *>(objPtr);

        int *pixels = static_cast<int *>(malloc(cropWidth * cropHeight * sizeof(int)));
        imageUtil.convertNV21ToGrayAndScale(left, top, cropWidth, cropHeight, rowWidth, bytes,
                                            pixels);

        auto binImage = BinaryBitmapFromBytes(env, pixels, 0, 0, cropWidth, cropHeight);
        auto readResult = reader->read(*binImage);

        if (readResult.isValid()) {
            free(pixels);

            env->SetObjectArrayElement(result, 0, ToJavaString(env, readResult.text()));
            env->SetObjectArrayElement(result, 1, ToJavaArray(env, readResult.resultPoints()));
            return static_cast<int>(readResult.format());
        } else {
            OpencvProcessor opencvProcessor;
            cv::Rect rect;
            opencvProcessor.processData(pixels, cropWidth, cropHeight, &rect);
            free(pixels);
            if (!rect.empty()) {
                env->SetObjectArrayElement(result, 2, reactToJavaArray(env, rect));
                return 1;
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