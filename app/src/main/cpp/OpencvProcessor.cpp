//
// Created by Devilsen on 2019/07/14 0014.
//

#include <opencv2/imgproc/types_c.h>
#include "OpencvProcessor.h"
#include "opencv2/opencv.hpp"
#include "JNIUtils.h"

using namespace cv;
using namespace std;

void OpencvProcessor::processData(int *data, jint w, jint h, Point *point) {

    Mat src(h, w, CV_8UC4, data);
//    cvtColor(src, src, COLOR_YUV2RGBA_NV21);

    Mat gray, binary;
    cvtColor(src, gray, COLOR_BGR2GRAY);
    equalizeHist(gray, gray);
    imwrite("/storage/emulated/0/scan/src_gray.jpg", gray);
    // 进行canny化，变成黑白线条构成的图片
    Canny(gray, binary, 100 , 255, 3);
    imwrite("/storage/emulated/0/scan/src_canny.jpg", binary);
//    不能加这个
//    blur(gray, binary, Size(3, 3));
    // 二值化
//    threshold(gray, binary, 100, 255, THRESH_BINARY | THRESH_OTSU);
//    imwrite("/storage/emulated/0/scan/src_binary.jpg", binary);

    // detect rectangle now
    vector<vector<Point>> contours;
    vector<Vec4i> hireachy;
    findContours(binary, contours, hireachy, RETR_TREE, CHAIN_APPROX_SIMPLE);
    Mat result = Mat::zeros(src.size(), CV_8UC4);
    for (size_t t = 0; t < contours.size(); t++) {
        double area = contourArea(contours[t]);

        if (area < 100) continue;
        RotatedRect rect = minAreaRect(contours[t]);
        // 根据矩形特征进行几何分析
        float w = rect.size.width;
        float h = rect.size.height;
        float rate = min(w, h) / max(w, h);
        if (rate > 0.85 && w < src.cols / 4 && h < src.rows / 4) {
//            LOGE("angle : %.2f\n ", rect.angle);
            LOGE("size left: %f ,top %f , width %f ,height %f ", rect.center.x, rect.center.y, w,
                 h);
//            Mat qr_roi = transformCorner(src, rect);
//            if (isCorner(qr_roi)) {
            drawContours(src, contours, static_cast<int>(t), Scalar(255, 0, 0), 2, 8);
//                imwrite(format("/storage/emulated/0/scan/src_%d.jpg", static_cast<int>(t)), qr_roi);
            drawContours(result, contours, static_cast<int>(t), Scalar(255, 0, 0), 2, 8);
//            }
        }
    }
    imwrite("/storage/emulated/0/scan/src_patter.jpg", result);
}