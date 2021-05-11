#include <jni.h>
#include <string>
#include <vector>
#include <src/BitMatrix.h>
#include "JNIUtils.h"
#include "MultiFormatWriter.h"
#include "ImageScheduler.h"

JavaVM *javaVM = nullptr;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    javaVM = vm;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR; // JNI version not supported.
    }

    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_me_devilsen_czxing_code_NativeSdk_createInstance(JNIEnv *env, jobject instance,
                                                      jintArray formats_) {
    try {
        ZXing::DecodeHints hints;
        if (formats_ != nullptr) {
            hints.setPossibleFormats(GetFormats(env, formats_));
        }

        auto *imageScheduler = new ImageScheduler(hints);
        return reinterpret_cast<jlong>(imageScheduler);
    } catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    } catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_me_devilsen_czxing_code_NativeSdk_setDetectModel(JNIEnv* env, jobject thiz, jlong objPtr,
                                                      jstring detector_prototxt_path,
                                                      jstring detector_caffe_model_path,
                                                      jstring super_resolution_prototxt_path,
                                                      jstring super_resolution_caffe_model_path)
{
    const char *detectorPrototxtPath = env->GetStringUTFChars(detector_prototxt_path, 0);
    const char *detectorCaffeModelPath = env->GetStringUTFChars(detector_caffe_model_path, 0);
    const char *superResolutionPrototxtPath = env->GetStringUTFChars(super_resolution_prototxt_path, 0);
    const char *superResolutionCaffeModelPath = env->GetStringUTFChars(super_resolution_caffe_model_path, 0);

    auto imageScheduler = reinterpret_cast<ImageScheduler *>(objPtr);
    imageScheduler->setWeChatDetect(detectorPrototxtPath, detectorCaffeModelPath,
                                   superResolutionPrototxtPath, superResolutionCaffeModelPath);

    env->ReleaseStringUTFChars(detector_prototxt_path, detectorPrototxtPath);
    env->ReleaseStringUTFChars(detector_caffe_model_path, detectorCaffeModelPath);
    env->ReleaseStringUTFChars(super_resolution_prototxt_path, superResolutionPrototxtPath);
    env->ReleaseStringUTFChars(super_resolution_caffe_model_path, superResolutionCaffeModelPath);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_devilsen_czxing_code_NativeSdk_destroyInstance(JNIEnv *env, jobject instance,
                                                       jlong objPtr) {
    if (objPtr == 0) {
        return;
    }
    auto imageScheduler = reinterpret_cast<ImageScheduler *>(objPtr);
    DELETE(imageScheduler);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_devilsen_czxing_code_NativeSdk_setFormat(JNIEnv *env, jobject thiz, jlong objPtr,
                                                 jintArray formats_) {
    if (objPtr == 0) {
        return;
    }
    auto imageScheduler = reinterpret_cast<ImageScheduler *>(objPtr);
    imageScheduler->setFormat(env, formats_);
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_code_NativeSdk_readByte(JNIEnv *env, jobject instance, jlong objPtr,
                                                       jbyteArray bytes_, jint left, jint top,
                                                       jint cropWidth, jint cropHeight,
                                                       jint rowWidth, jint rowHeight,
                                                       jobjectArray result) {
    jbyte *bytes = env->GetByteArrayElements(bytes_, nullptr);

    auto imageScheduler = reinterpret_cast<ImageScheduler *>(objPtr);
    auto readResult = imageScheduler->readByte(env, bytes, left, top, cropWidth, cropHeight,
                                               rowWidth, rowHeight);
    env->ReleaseByteArrayElements(bytes_, bytes, 0);
    return processResult(env, readResult, result);
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_code_NativeSdk_readBitmap(JNIEnv *env, jobject instance, jlong objPtr,
                                                  jobject bitmap, jint left, jint top, jint width,
                                                  jint height, jobjectArray result) {

    auto imageScheduler = reinterpret_cast<ImageScheduler *>(objPtr);
    auto readResult = imageScheduler->readBitmap(env, bitmap, left, top, width, height);
    return processResult(env, readResult, result);
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_code_NativeSdk_readFullBitmap(JNIEnv *env, jobject instance,
                                                      jlong objPtr, jobject bitmap,
                                                      jobjectArray result) {
    auto imageScheduler = reinterpret_cast<ImageScheduler *>(objPtr);
    auto readResult = imageScheduler->readBitmap(env, bitmap);
    return processResult(env, readResult, result);
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_code_NativeSdk_writeCode(JNIEnv *env, jobject instance, jstring content_,
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