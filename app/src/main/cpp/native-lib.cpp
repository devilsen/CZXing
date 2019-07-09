#include <jni.h>
#include <string>
#include "JNIUtils.h"
#include "MultiFormatReader.h"
#include "DecodeHints.h"
#include "Result.h"

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
            if (!readResult.resultPoints().empty()) {
                jfloatArray array = ToJavaArray(env, readResult.resultPoints());
                env->SetObjectArrayElement(result, 1, array);
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

bool checkSize(int left, int top, int width, int height) {
    if (left < 0) {
        left = 0;
    }
    if (top < 0) {
        top = 0;
    }
    if (left > width || top > height) {
        LOGE("input data error, left can't larger than width");
        return false;
    }
    return true;
}

void
convertNV21ToGrayScal(int left, int top, int width, int height, int rowWidth, const jbyte *data,
                      int *pixels) {
    int p;
    int desIndex = 0;
    int bottom = top + height;
    int right = left + width;
    int srcIndex = top * rowWidth;
    int marginRight = rowWidth - left - width;
    for (int i = top; i < bottom; ++i) {
        srcIndex += left;
        for (int j = left; j < right; ++j, ++desIndex, ++srcIndex) {
            p = data[srcIndex] & 0xFF;
            pixels[desIndex] = 0xff000000u | p << 16u | p << 8u | p;
        }
        srcIndex += marginRight;
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_BarcodeReader_readBarcodeByte(JNIEnv *env, jclass type, jlong objPtr,
                                                      jbyteArray bytes_, jint left, jint top,
                                                      jint cropWidth, jint cropHeight,
                                                      jint rowWidth,
                                                      jobjectArray result) {
    jbyte *bytes = env->GetByteArrayElements(bytes_, NULL);

    if (!checkSize(left, top, cropWidth, cropHeight)) {
        return -1;
    }

    try {
        auto reader = reinterpret_cast<ZXing::MultiFormatReader *>(objPtr);

        int *pixels = static_cast<int *>(malloc(cropWidth * cropHeight * sizeof(int)));
        convertNV21ToGrayScal(left, top, cropWidth, cropHeight, rowWidth, bytes, pixels);

        auto binImage = BinaryBitmapFromBytes(env, pixels, 0, 0, cropWidth, cropHeight);
        auto readResult = reader->read(*binImage);
        free(pixels);

        if (readResult.isValid()) {
            env->SetObjectArrayElement(result, 0, ToJavaString(env, readResult.text()));
            return static_cast<int>(readResult.format());
        } else if (readResult.isBlurry()) {
            env->SetObjectArrayElement(result, 1, ToJavaArray(env, readResult.resultPoints()));
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
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_BarcodeReader_readBarcodeByteFullImage(JNIEnv *env, jclass type,
                                                               jlong objPtr, jbyteArray bytes_,
                                                               jint width, jint height,
                                                               jobjectArray result) {
    jbyte *bytes = env->GetByteArrayElements(bytes_, NULL);

    try {
        auto reader = reinterpret_cast<ZXing::MultiFormatReader *>(objPtr);

        int *pixels = static_cast<int *>(malloc(width * height * sizeof(int)));
        convertNV21ToGrayScal(0, 0, width, height, width, bytes, pixels);

        auto binImage = BinaryBitmapFromBytes(env, pixels, 0, 0, width, height);
        auto readResult = reader->read(*binImage);
        free(pixels);

        if (readResult.isValid()) {
            env->SetObjectArrayElement(result, 0, ToJavaString(env, readResult.text()));
            if (!readResult.resultPoints().empty()) {
                jfloatArray array = ToJavaArray(env, readResult.resultPoints());
                env->SetObjectArrayElement(result, 1, array);
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