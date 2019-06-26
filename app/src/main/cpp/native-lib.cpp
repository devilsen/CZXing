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

void NV21_T_RGB(int width, int height, const jbyte *yuv,jbyte *rgb) {
    const int nv_start = width * height;
    uint32_t i, j, index = 0, rgb_index = 0;
    jbyte y, u, v;
    int r, g, b, nv_index = 0;


    for (i = 0; i < height; i++) {
        for (j = 0; j < width; j++) {
            //nv_index = (rgb_index / 2 - width / 2 * ((i + 1) / 2)) * 2;
            nv_index = i / 2 * width + j - j % 2;

            y = yuv[rgb_index];
            u = yuv[nv_start + nv_index];
            v = yuv[nv_start + nv_index + 1];


            r = y + (140 * (v - 128)) / 100;  //r
            g = y - (34 * (u - 128)) / 100 - (71 * (v - 128)) / 100; //g
            b = y + (177 * (u - 128)) / 100; //b

            if (r > 255) r = 255;
            if (g > 255) g = 255;
            if (b > 255) b = 255;
            if (r < 0) r = 0;
            if (g < 0) g = 0;
            if (b < 0) b = 0;

            index = rgb_index % width + (height - i - 1) * width;
            rgb[index * 3 + 0] = b;
            rgb[index * 3 + 1] = g;
            rgb[index * 3 + 2] = r;
            rgb_index++;
        }
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

        void *rgb = nullptr;
        NV21_T_RGB(width , height, bytes, static_cast<jbyte *>(rgb));
        auto binImage = BinaryBitmapFromBytes(env, rgb, left, top, width, height);
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
    env->ReleaseByteArrayElements(bytes_, bytes, 0);

    return -1;
}