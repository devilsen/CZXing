//
// Created by Devilsen on 2019-07-19.
//

#ifndef CZXING_IMAGEUTIL_H
#define CZXING_IMAGEUTIL_H


#include <jni.h>
#include <opencv2/core/types.hpp>

class ImageUtil {
public:
    /**
     *  检查输入图片的边界
     * @param left
     * @param top
     * @return
     */
    bool checkSize(int *left, int *top);

    /**
     * 把图片转化为灰度图，并进行裁剪
     * @param left 左边界
     * @param top  上边界
     * @param width 要截取的图片宽
     * @param height 要截取的图片高
     * @param rowWidth 输入文件的宽度
     * @param data 输入文件
     * @param pixels 输出文件
     */
    void convertNV21ToGrayScaleRotate(int left, int top, int width, int height, int rowWidth,
                                      const jbyte *data, int *pixels);


    /**
     * 裁剪opencv返回的图像数据
     * @param rect_ 图像数据
     * @param rowWidth 图像宽
     * @param pixels 输入参数
     * @param scalePixels  输出参数
     */
    void scaleImage(const cv::Rect &rect_, int rowWidth, const int *pixels, int *scalePixels);
};


#endif //CZXING_IMAGEUTIL_H
