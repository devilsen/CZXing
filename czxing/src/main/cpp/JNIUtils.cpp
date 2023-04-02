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
#include "JNIUtils.h"
#include "HybridBinarizer.h"

#include <android/bitmap.h>
#include <stdexcept>
#include <vector>
#include <opencv2/core/types.hpp>
#include <opencv2/core/mat.hpp>
#include <opencv2/imgproc.hpp>
#include <src/TextUtfEncoding.h>
#include <src/BarcodeFormat.h>

namespace {

    struct AutoUnlockPixels {
        JNIEnv *m_env;
        jobject m_bitmap;

        AutoUnlockPixels(JNIEnv *env, jobject bitmap) : m_env(env), m_bitmap(bitmap) {}

        ~AutoUnlockPixels() {
            AndroidBitmap_unlockPixels(m_env, m_bitmap);
        }
    };

} // anonymous

void
BitmapToMat(JNIEnv *env, jobject bitmap, cv::Mat &mat) {
    AndroidBitmapInfo bmInfo;
    if (AndroidBitmap_getInfo(env, bitmap, &bmInfo) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("nBitmapToMat: get bitmap info error");
        return;
    };

    void *pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) == ANDROID_BITMAP_RESUT_SUCCESS) {
        AutoUnlockPixels autounlock(env, bitmap);

        if (bmInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            LOGE("nBitmapToMat: RGB_8888 -> CV_8UC4");
            cv::Mat tmp(bmInfo.height, bmInfo.width, CV_8UC4, pixels);
            tmp.copyTo(mat);
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            LOGE("nBitmapToMat: RGB_565 -> CV_8UC4");
            cv::Mat tmp(bmInfo.height, bmInfo.width, CV_8UC4, pixels);
            tmp.copyTo(mat);
        }
    } else {
        throw std::runtime_error("Failed to read bitmap's data");
    }
}

/**
 * wstring转为string
 */
std::string UnicodeToANSI(const std::wstring &wstr) {
    return ZXing::TextUtfEncoding::ToUtf8(wstr);
}

/**
 * string转为wstring
 */
std::wstring ANSIToUnicode(const std::string &str) {
    return ZXing::TextUtfEncoding::FromUtf8(str);
}

void ThrowJavaException(JNIEnv *env, const char *message) {
    static jclass jcls = env->FindClass("java/lang/RuntimeException");
    env->ThrowNew(jcls, message);
}

static bool RequiresSurrogates(uint32_t ucs4) {
    return ucs4 >= 0x10000;
}

static uint16_t HighSurrogate(uint32_t ucs4) {
    return uint16_t((ucs4 >> 10) + 0xd7c0);
}

static uint16_t LowSurrogate(uint32_t ucs4) {
    return uint16_t(ucs4 % 0x400 + 0xdc00);
}

static void Utf32toUtf16(const uint32_t *utf32, size_t length, std::vector<uint16_t> &result) {
    result.clear();
    result.reserve(length);
    for (size_t i = 0; i < length; ++i) {
        uint32_t c = utf32[i];
        if (RequiresSurrogates(c)) {
            result.push_back(HighSurrogate(c));
            result.push_back(LowSurrogate(c));
        } else {
            result.push_back(c);
        }
    }
}

jstring wstring2jstring(JNIEnv *env, const std::wstring &str) {
    std::vector<uint16_t> buffer;
    Utf32toUtf16((const uint32_t*) str.data(), str.size(), buffer);
    return env->NewString((const jchar*) buffer.data(), buffer.size());
}

std::string jstring2string(JNIEnv *env, jstring str) {
    if (str) {
        const char *kstr = env->GetStringUTFChars(str, nullptr);
        if (kstr) {
            std::string result(kstr);
            env->ReleaseStringUTFChars(str, kstr);
            return result;
        }
    }
    return "";
}

jstring string2jstring(JNIEnv *env, const std::string &str) {
    return env->NewStringUTF(str.c_str());
}
