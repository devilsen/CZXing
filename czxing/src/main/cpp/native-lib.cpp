#include <jni.h>
#include <string>
#include <vector>
#include <src/BitMatrix.h>
#include "JNIUtils.h"
#include "MultiFormatWriter.h"
#include "DecodeScheduler.h"

extern "C"
JNIEXPORT jlong JNICALL
Java_me_devilsen_czxing_code_DecodeEngine_nativeCreateInstance(JNIEnv *env, jobject instance,
                                                               jintArray formats_) {
    try {
        auto *imageScheduler = new czxing::DecodeScheduler();
        if (formats_ != nullptr) {
            imageScheduler->setFormat(env, formats_);
        }
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
Java_me_devilsen_czxing_code_DecodeEngine_nativeDestroyInstance(JNIEnv *env, jobject instance,
                                                                jlong objPtr) {
    if (objPtr == 0) {
        return;
    }
    auto imageScheduler = reinterpret_cast<czxing::DecodeScheduler *>(objPtr);
    DELETE(imageScheduler);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_devilsen_czxing_code_DecodeEngine_nativeSetDetectModel(JNIEnv* env, jobject thiz, jlong objPtr,
                                                               jstring detector_prototxt_path,
                                                               jstring detector_caffe_model_path,
                                                               jstring super_resolution_prototxt_path,
                                                               jstring super_resolution_caffe_model_path)
{
    const char *detectorPrototxtPath = env->GetStringUTFChars(detector_prototxt_path, 0);
    const char *detectorCaffeModelPath = env->GetStringUTFChars(detector_caffe_model_path, 0);
    const char *superResolutionPrototxtPath = env->GetStringUTFChars(super_resolution_prototxt_path, 0);
    const char *superResolutionCaffeModelPath = env->GetStringUTFChars(super_resolution_caffe_model_path, 0);

    auto imageScheduler = reinterpret_cast<czxing::DecodeScheduler *>(objPtr);
    if (!imageScheduler) {
        return;
    }
    imageScheduler->setWeChatDetect(detectorPrototxtPath, detectorCaffeModelPath,
                                    superResolutionPrototxtPath, superResolutionCaffeModelPath);

    env->ReleaseStringUTFChars(detector_prototxt_path, detectorPrototxtPath);
    env->ReleaseStringUTFChars(detector_caffe_model_path, detectorCaffeModelPath);
    env->ReleaseStringUTFChars(super_resolution_prototxt_path, superResolutionPrototxtPath);
    env->ReleaseStringUTFChars(super_resolution_caffe_model_path, superResolutionCaffeModelPath);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_devilsen_czxing_code_DecodeEngine_nativeSetFormat(JNIEnv *env, jobject thiz, jlong objPtr,
                                                          jintArray formats_) {
    if (objPtr == 0) {
        return;
    }
    auto imageScheduler = reinterpret_cast<czxing::DecodeScheduler *>(objPtr);
    if (imageScheduler) {
        imageScheduler->setFormat(env, formats_);
    }
}

jobjectArray packageJavaArray(JNIEnv *env, std::vector<czxing::ScanResult> resultVector) {
    if (resultVector.empty()) return nullptr;

    int size = resultVector.size();
    auto jResult = czxing::ScanResult::obtainResultArray(env, size);
    for (int i = 0; i < size; ++i) {
        env->SetObjectArrayElement(jResult, i, resultVector[i].getJCodeResult(env));
    }
    return jResult;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_me_devilsen_czxing_code_DecodeEngine_nativeDecodeByte(JNIEnv *env, jobject instance, jlong objPtr,
                                                           jbyteArray bytes_, jint left, jint top,
                                                           jint cropWidth, jint cropHeight,
                                                           jint rowWidth, jint rowHeight) {
    if (objPtr == 0) {
        return nullptr;
    }

    if (bytes_ == nullptr) {
        return nullptr;
    }

    auto imageScheduler = reinterpret_cast<czxing::DecodeScheduler *>(objPtr);
    if (!imageScheduler){
        return nullptr;
    }
    jbyte *bytes = env->GetByteArrayElements(bytes_, nullptr);
    auto readResult = imageScheduler->readByte(bytes, rowWidth, rowHeight);
    env->ReleaseByteArrayElements(bytes_, bytes, 0);
    return packageJavaArray(env, readResult);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_me_devilsen_czxing_code_DecodeEngine_nativeDecodeBitmap(JNIEnv *env, jobject instance, jlong objPtr, jobject bitmap) {
    if (objPtr == 0) {
        return nullptr;
    }

    auto imageScheduler = reinterpret_cast<czxing::DecodeScheduler *>(objPtr);
    if (!imageScheduler){
        return nullptr;
    }
    auto readResult = imageScheduler->readBitmap(env, bitmap);
    return packageJavaArray(env, readResult);
}

extern "C"
JNIEXPORT jdouble JNICALL
Java_me_devilsen_czxing_code_DecodeEngine_nativeDetectBrightness(JNIEnv* env, jobject thiz, jlong objPtr,
                                                                 jbyteArray bytes_, jint rowWidth,
                                                                 jint rowHeight)
{
    if (objPtr == 0) {
        return -1;
    }

    if (bytes_ == nullptr) {
        return -1;
    }

    auto imageScheduler = reinterpret_cast<czxing::DecodeScheduler *>(objPtr);
    if (!imageScheduler) {
        return -1;
    }

    jbyte *bytes = env->GetByteArrayElements(bytes_, nullptr);
    auto brightness = imageScheduler->detectBrightness(bytes, rowWidth, rowHeight);
    env->ReleaseByteArrayElements(bytes_, bytes, 0);
    return brightness;
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_code_EncodeEngine_nativeWriteCode(JNIEnv *env, jobject instance, jstring content_,
                                                          jint width, jint height, jint color,
                                                          jstring format_, jobjectArray result) {
    const char *content = env->GetStringUTFChars(content_, 0);
    const char *format = env->GetStringUTFChars(format_, 0);

    auto zxingFormat = ZXing::BarcodeFormatFromString(format);
    ZXing::CharacterSet encoding = ZXing::CharacterSet::UTF8;
    auto writer = ZXing::MultiFormatWriter(zxingFormat).setEncoding(encoding).setEccLevel(5);
    ZXing::BitMatrix bitMatrix = writer.encode(content, width, height);

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
    return 0;
}