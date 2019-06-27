#include <jni.h>
#include <string>
#include "JNIUtils.h"
#include "MultiFormatReader.h"
#include "DecodeHints.h"
#include "Result.h"
#include "yuv2bmp.h"

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

extern "C" JNIEXPORT jstring JNICALL
Java_me_devilsen_czxing_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
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
}extern "C"
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
}extern "C"
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

void applyGrayScale(int *pixels, const jbyte *data, int width, int height) {
    int p;
    int size = width * height;
    for (int i = 0; i < size; i++) {
        p = data[i] & 0xFF;
        pixels[i] = 0xff000000 | p << 16u | p << 8u | p;
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_BarcodeReader_readBarcodeByte(JNIEnv *env, jclass type, jlong objPtr,
                                                      jbyteArray bytes_, jint left, jint top,
                                                      jint width, jint height,
                                                      jobjectArray result) {
    jbyte *bytes = env->GetByteArrayElements(bytes_, NULL);

    try {
        auto reader = reinterpret_cast<ZXing::MultiFormatReader *>(objPtr);

        int *grayscale = static_cast<int *>(malloc(width * height * sizeof(uint32_t)));
        applyGrayScale(grayscale, bytes, width, height);

        LOGE("get GrayScale success");

        auto binImage = BinaryBitmapFromBytes(env, grayscale, left, top, width, height);
        auto readResult = reader->read(*binImage);
        free(grayscale);

        if (readResult.isValid()) {
            env->SetObjectArrayElement(result, 0, ToJavaString(env, readResult.text()));
            return static_cast<int>(readResult.format());
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
}extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_BarcodeReader_readBarcodeByte2(JNIEnv *env, jclass type, jlong objPtr,
                                                       jintArray bytes_, jint left, jint top,
                                                       jint width, jint height,
                                                       jobjectArray result) {
    jint *bytes = env->GetIntArrayElements(bytes_, NULL);

    try {
        auto reader = reinterpret_cast<ZXing::MultiFormatReader *>(objPtr);

        auto binImage = BinaryBitmapFromBytes(env, bytes, left, top, width, height);
        auto readResult = reader->read(*binImage);

        LOGE("get readResult success");

        if (readResult.isValid()) {
            LOGE("isValid success");

            env->SetObjectArrayElement(result, 0, ToJavaString(env, readResult.text()));
            return static_cast<int>(readResult.format());
        }
    }
    catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    }
    catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }
    env->ReleaseIntArrayElements(bytes_, bytes, 0);

    return -1;
}