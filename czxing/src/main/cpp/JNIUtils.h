#pragma once
/*
* Copyright 2016 Nu-book Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

#include <jni.h>
#include <android/log.h>

#include <memory>
#include <string>
#include <src/ResultPoint.h>
#include <opencv2/core/types.hpp>
#include <src/BarcodeFormat.h>
#include <src/Result.h>
#include "ScanResult.h"

#define ZX_LOG_TAG "CZXing"
//#define DEBUG

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, ZX_LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, ZX_LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, ZX_LOG_TAG, __VA_ARGS__)

#ifdef DEBUG
#define LOGE(...)                                                                                  \
    __android_log_print(ANDROID_LOG_ERROR, ZX_LOG_TAG, __VA_ARGS__);
#else
#define LOGE(...)                                                                                  \
{}
#endif

#define DELETE(obj) if(obj){ delete obj; obj = 0; }

namespace ZXing {
    class BinaryBitmap;
}

// Create BinaryBitmap from Android's Bitmap
std::shared_ptr<ZXing::BinaryBitmap>
BinaryBitmapFromJavaBitmap(JNIEnv *env, jobject bitmap, int cropLeft, int cropTop, int cropWidth,
                           int cropHeight);

std::shared_ptr<ZXing::BinaryBitmap>
BinaryBitmapFromBytesC4(void *rgbScale, int cropLeft, int cropTop, int cropWidth,
                        int cropHeight);

std::shared_ptr<ZXing::BinaryBitmap>
BinaryBitmapFromBytesC1(void *grayScale, int cropLeft, int cropTop, int cropWidth, int cropHeight);

void
BitmapToMat(JNIEnv *env, jobject bitmap, cv::Mat &mat);

std::string UnicodeToANSI(const std::wstring &wstr);

std::wstring ANSIToUnicode(const std::string &src);

void ThrowJavaException(JNIEnv *env, const char *message);

jstring wstring2jstring(JNIEnv *env, const std::wstring &str);

std::string jstring2string(JNIEnv *env, jstring str);

jstring string2jstring(JNIEnv *env, const std::string &str);

jfloatArray ToJavaArray(JNIEnv *env, const std::vector<ZXing::ResultPoint> &vector);

jintArray rect2JavaArray(JNIEnv *env, const czxing::CodeRect codeRect);

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

static int processResult(JNIEnv *env, std::vector<czxing::ScanResult> resultVector, jobjectArray result) {
    if (resultVector.empty()) return -1;

    for (int i = 0; i < resultVector.size(); ++i) {
        czxing::ScanResult scanResult = resultVector[i];

//        env->SetObjectArrayElement(result, 0, reinterpret_cast<jobject>(scanResult.format()));
//        env->SetObjectArrayElement(result, 1, string2jstring(env, scanResult.text()));
//        env->SetObjectArrayElement(result, 2, rect2JavaArray(env, scanResult.rect()));
    }
    return 0;
}