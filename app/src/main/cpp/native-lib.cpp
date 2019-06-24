#include <jni.h>
#include <string>
#include "JNIUtils.h"
#include "include/MultiFormatReader.h"
#include "include/DecodeHints.h"
#include "include/Result.h"

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