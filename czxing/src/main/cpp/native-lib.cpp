#include <jni.h>
#include <string>
#include "JNIUtils.h"
#include "MultiFormatReader.h"
#include "DecodeHints.h"
#include "Result.h"
#include "QRCodeRecognizer.h"
#include "opencv2/opencv.hpp"
#include <vector>
#include <opencv2/imgproc/types_c.h>
#include "MultiFormatWriter.h"
#include "BitMatrix.h"
#include "ImageScheduler.h"
#include "JavaCallHelper.h"
#include <sys/time.h>

JavaCallHelper *javaCallHelper;
JavaVM *javaVM = nullptr;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    javaVM = vm;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR; // JNI version not supported.
    }

    return JNI_VERSION_1_6;
}

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
Java_me_devilsen_czxing_code_NativeSdk_createInstance(JNIEnv *env, jobject instance,
                                                      jintArray formats_) {
    try {
        if (javaCallHelper) {
            DELETE(javaCallHelper);
        }
        javaCallHelper = new JavaCallHelper(javaVM, env, instance);

        ZXing::DecodeHints hints;
        if (formats_ != nullptr) {
            hints.setPossibleFormats(GetFormats(env, formats_));
        }

        auto *multiFormatReader = new ZXing::MultiFormatReader(hints);
        auto *imageScheduler = new ImageScheduler(env, multiFormatReader, javaCallHelper);
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
Java_me_devilsen_czxing_code_NativeSdk_setFormat(JNIEnv *env, jobject thiz, jlong objPtr,
                                                 jintArray formats_) {
    if (objPtr == 0) {
        return;
    }
    auto imageScheduler = reinterpret_cast<ImageScheduler *>(objPtr);
    ZXing::DecodeHints hints;
    bool decodeQr = true;
    if (formats_ != nullptr) {
        std::vector<ZXing::BarcodeFormat> formats = GetFormats(env, formats_);
        hints.setPossibleFormats(formats);

        if (std::find(formats.begin(), formats.end(), ZXing::BarcodeFormat::QR_CODE) ==
            formats.end()) {
            decodeQr = false;
        }
    }
    imageScheduler->isDecodeQrCode(decodeQr);
    imageScheduler->reader->setFormat(hints);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_devilsen_czxing_code_NativeSdk_destroyInstance(JNIEnv *env, jobject instance,
                                                       jlong objPtr) {

    try {
        auto imageScheduler = reinterpret_cast<ImageScheduler *>(objPtr);
        imageScheduler->stop();
        delete imageScheduler;
        DELETE(javaCallHelper);
    }
    catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    }
    catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_me_devilsen_czxing_code_NativeSdk_prepareRead(JNIEnv *env, jobject thiz, jlong objPtr) {
    auto imageScheduler = reinterpret_cast<ImageScheduler *>(objPtr);
    imageScheduler->prepare();
}

extern "C"
JNIEXPORT void JNICALL
Java_me_devilsen_czxing_code_NativeSdk_stopRead(JNIEnv *env, jobject thiz, jlong objPtr) {
    auto imageScheduler = reinterpret_cast<ImageScheduler *>(objPtr);
    imageScheduler->stop();
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_code_NativeSdk_readBarcodeByte(JNIEnv *env, jobject instance, jlong objPtr,
                                                       jbyteArray bytes_, jint left, jint top,
                                                       jint cropWidth, jint cropHeight,
                                                       jint rowWidth, jint rowHeight) {
    jbyte *bytes = env->GetByteArrayElements(bytes_, NULL);

    auto imageScheduler = reinterpret_cast<ImageScheduler *>(objPtr);
    imageScheduler->process(bytes, left, top, cropWidth, cropHeight, rowWidth, rowHeight);

    env->ReleaseByteArrayElements(bytes_, bytes, 0);
    return -1;
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_code_NativeSdk_readBarcode(JNIEnv *env, jobject instance, jlong objPtr,
                                                   jobject bitmap, jint left, jint top, jint width,
                                                   jint height, jobjectArray result) {

    try {
        auto imageScheduler = reinterpret_cast<ImageScheduler *>(objPtr);
        auto readResult = imageScheduler->readBitmap(bitmap, left, top, width, height);
        if (readResult.isValid()) {
            env->SetObjectArrayElement(result, 0, ToJavaString(env, readResult.text()));
            if (!readResult.resultPoints().empty()) {
                env->SetObjectArrayElement(result, 1, ToJavaArray(env, readResult.resultPoints()));
            }
            return static_cast<int>(readResult.format());
        }
    } catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    } catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }
    return -1;
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