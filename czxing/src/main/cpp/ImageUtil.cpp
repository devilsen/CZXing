//
// Created by Devilsen on 2019-07-19.
//

#include "ImageUtil.h"

bool ImageUtil::checkSize(int *left, int *top) {
    if (*left < 0) {
        *left = 0;
    }
    if (*top < 0) {
        *top = 0;
    }
    return true;
}

void ImageUtil::convertNV21ToGrayAndScale(int left, int top, int width, int height, int rowWidth,
                                          const jbyte *data, int *pixels) {
    int p;
    int desIndex = 0;
    int bottom = top + height;
    int right = left + width;
    int srcIndex = top * rowWidth;
    int marginRight = rowWidth - right;
    for (int i = top; i < bottom; ++i) {
        srcIndex += left;
        for (int j = left; j < right; ++j, ++desIndex, ++srcIndex) {
            p = data[srcIndex] & 0xFF;
            pixels[desIndex] = 0xff000000u | p << 16u | p << 8u | p;
        }
        srcIndex += marginRight;
    }
}

void
ImageUtil::scaleImage(const cv::Rect &rect_, int rowWidth, const int *pixels, int *scalePixels) {
    int left = rect_.x;
    int top = rect_.y;
    int right = left + rect_.width;
    int bottom = top + rect_.height;
    int marginRight = rowWidth - right;

    int desIndex = 0;
    int srcIndex = top * rowWidth;
    for (int i = top; i < bottom; ++i) {
        srcIndex += left;
        for (int j = left; j < right; ++j, ++desIndex, ++srcIndex) {
            scalePixels[desIndex] = pixels[srcIndex];
        }
        srcIndex += marginRight;
    }
}
