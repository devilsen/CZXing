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

void NV21_T_RGB(unsigned int width, unsigned int height, unsigned char *yuyv, unsigned char *rgb) {
    const int nv_start = width * height;
    uint32_t i;
    uint32_t j;
    uint32_t index = 0;
    uint32_t rgb_index = 0;
    uint8_t y;
    uint8_t u;
    uint8_t v;
    int r;
    int g;
    int b;
    int nv_index = 0;

    for (i = 0; i < height; ++i) {
        for (j = 0; j < width; ++j) {
            //nv_index = (rgb_index / 2 - width / 2 * ((i + 1) / 2)) * 2;
            nv_index = i / 2 * width + j - j % 2;

            y = yuyv[rgb_index];
            u = yuyv[nv_start + nv_index];
            v = yuyv[nv_start + nv_index + 1];

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