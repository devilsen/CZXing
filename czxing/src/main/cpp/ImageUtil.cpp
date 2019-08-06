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

void ImageUtil::convertNV21ToGrayScaleRotate(int left, int top, int width, int height, int rowWidth,
                                             const jbyte *data, int *pixels) {
    int p;
    int desIndex = 0;
    int bottom = top + height;
    int right = left + width;
    int srcIndex = 0;
    for (int i = left; i < right; ++i) {
        srcIndex = (bottom - 1) * rowWidth + i;
        for (int j = 0; j < height; ++j, ++desIndex, srcIndex -= rowWidth) {
            p = data[srcIndex] & 0xFF;
            pixels[desIndex] = 0xff000000u | p << 16u | p << 8u | p;
        }
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

void ImageUtil::binaryzation(int width, int height, int *pixels) {
    int index = 0;
    for (int i = 0; i < height; ++i) {
        for (int j = 0; j < width; ++j) {
            int p = pixels[index];
            // 得到Alpha通道的值
            int alpha = p & 0xFF000000;
            // 得到Red的值
            int red = (p & 0x00FF0000) >> 16;
            // 得到Green的值
            int green = (p & 0x0000FF00) >> 8;
            // 得到Blue的值
            int blue = p & 0x000000FF;

            // 通过加权平均算法,计算出最佳像素值
            int gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
            // 对图像设置黑白图
            if (gray <= 95) {
                gray = 0;
            } else {
                gray = 255;
            }
            // 得到新的像素值
            pixels[index] = alpha | (gray << 16) | (gray << 8) | gray;
            index++;
        }
    }
}
