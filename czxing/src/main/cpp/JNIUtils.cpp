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
#include "GenericLuminanceSource.h"
#include "HybridBinarizer.h"

#include <android/bitmap.h>
#include <stdexcept>
#include <vector>
#include <opencv2/core/types.hpp>
#include <locale.h>

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

std::shared_ptr<ZXing::BinaryBitmap>
BinaryBitmapFromJavaBitmap(JNIEnv *env, jobject bitmap, int cropLeft, int cropTop, int cropWidth,
                           int cropHeight) {
    using namespace ZXing;

    AndroidBitmapInfo bmInfo;
    AndroidBitmap_getInfo(env, bitmap, &bmInfo);

    cropLeft = std::max(0, cropLeft);
    cropTop = std::max(0, cropTop);
    cropWidth = cropWidth < 0 ? ((int) bmInfo.width - cropLeft) : std::min(
            (int) bmInfo.width - cropLeft, cropWidth);
    cropHeight = cropHeight < 0 ? ((int) bmInfo.height - cropTop) : std::min(
            (int) bmInfo.height - cropTop, cropHeight);

    void *pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) == ANDROID_BITMAP_RESUT_SUCCESS) {
        AutoUnlockPixels autounlock(env, bitmap);

        std::shared_ptr<GenericLuminanceSource> luminance;
        switch (bmInfo.format) {
            case ANDROID_BITMAP_FORMAT_A_8:
                luminance = std::make_shared<GenericLuminanceSource>(cropLeft, cropTop, cropWidth,
                                                                     cropHeight, pixels,
                                                                     bmInfo.stride);
                break;
            case ANDROID_BITMAP_FORMAT_RGBA_8888:
                luminance = std::make_shared<GenericLuminanceSource>(cropLeft, cropTop, cropWidth,
                                                                     cropHeight, pixels,
                                                                     bmInfo.stride, 4, 0, 1, 2);
                break;
            default:
                LOGE("Unsupported format");
                return nullptr;
//				throw std::runtime_error("Unsupported format");
        }
        return std::make_shared<HybridBinarizer>(luminance);
    } else {
        throw std::runtime_error("Failed to read bitmap's data");
    }
}

std::shared_ptr<ZXing::BinaryBitmap>
BinaryBitmapFromBytes(JNIEnv *env, void *pixels, int cropLeft, int cropTop, int cropWidth,
                      int cropHeight) {
    using namespace ZXing;

//    LOGE("cropLeft %d , cropTop %d  cropWidth %d cropHeight %d", cropLeft, cropTop, cropWidth,
//         cropHeight);

    std::shared_ptr<GenericLuminanceSource> luminance = std::make_shared<GenericLuminanceSource>(
            cropLeft, cropTop, cropWidth,
            cropHeight, pixels,
            cropWidth * sizeof(int), 4, 0, 1, 2);

    return std::make_shared<HybridBinarizer>(luminance);
}

bool AnalysisBrightness(JNIEnv *env, const jbyte *bytes, int width, int height) {
    // 像素点的总亮度
    unsigned long pixelLightCount = 0L;
    // 像素点的总数
    int pixelCount = width * height;
    // 采集步长，因为没有必要每个像素点都采集，可以跨一段采集一个，减少计算负担，必须大于等于1。
    int step = 20;
    for (int i = 0; i < pixelCount; i += step) {
        // 如果直接加是不行的，因为 data[i] 记录的是色值并不是数值，byte 的范围是 +127 到 —128，
        pixelLightCount += bytes[i] & 0xffL;
    }
    // 平均亮度
    long cameraLight = pixelLightCount / (pixelCount / step);
    bool isDarkEnv = false;
//    LOGE("平均亮度 %ld", cameraLight);
    // 判断在时间范围 AMBIENT_BRIGHTNESS_WAIT_SCAN_TIME * lightSize 内是不是亮度过暗
    if (cameraLight < 60) {
        isDarkEnv = true;
    }

    return isDarkEnv;
}

/**
 * string转wstring
 */
std::wstring StringToWString(const std::string &src) {
    unsigned long len = src.size() * 2;  // 预留字节数
    setlocale(LC_CTYPE, "");        // 必须调用此函数,但是会造成污染
    auto *p = new wchar_t[len];     // 申请一段内存存放转换后的字符串
    mbstowcs(p, src.c_str(), len);  // 转换
    std::wstring desc(p);
    delete[] p;                     // 释放申请的内存
    return desc;
}

std::wstring ANSIToUnicode(const std::string &str) {
    std::wstring ret;
    std::mbstate_t state = {};
    const char *src = str.data();
    size_t len = std::mbsrtowcs(nullptr, &src, 0, &state);
    if (static_cast<size_t>(-1) != len) {
        std::unique_ptr<wchar_t[]> buff(new wchar_t[len + 1]);
        len = std::mbsrtowcs(buff.get(), &src, len, &state);
        if (static_cast<size_t>(-1) != len) {
            ret.assign(buff.get(), len);
        }
    }
    return ret;
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

jstring ToJavaString(JNIEnv *env, const std::wstring &str) {
    if (sizeof(wchar_t) == 2) {
        return env->NewString((const jchar *) str.data(), str.size());
    } else {
        std::vector<uint16_t> buffer;
        Utf32toUtf16((const uint32_t *) str.data(), str.size(), buffer);
        return env->NewString((const jchar *) buffer.data(), buffer.size());
    }
}

jfloatArray
ToJavaArray(JNIEnv *env, const std::vector<ZXing::ResultPoint> &input) {
    jfloatArray array = env->NewFloatArray(input.size() * 2);
    if (input.size() < 2) {
        return array;
    }

    int index = 0;
    for (auto point : input) {
        float x = point.x();
        float y = point.y();
        env->SetFloatArrayRegion(array, index++, 1, &x);
        env->SetFloatArrayRegion(array, index++, 1, &y);
    }

    return array;
}

jintArray
reactToJavaArray(JNIEnv *env, const cv::Rect &rect) {
    jintArray array = env->NewIntArray(6);

    cv::Point pointLeftTop = rect.tl();
    cv::Point pointRightBottom = rect.br();
    env->SetIntArrayRegion(array, 0, 1, &pointLeftTop.x);
    env->SetIntArrayRegion(array, 1, 1, &pointLeftTop.y);

    env->SetIntArrayRegion(array, 2, 1, &pointRightBottom.x);
    env->SetIntArrayRegion(array, 3, 1, &pointLeftTop.y);

    env->SetIntArrayRegion(array, 4, 1, &pointLeftTop.x);
    env->SetIntArrayRegion(array, 5, 1, &pointRightBottom.y);

    return array;
}
