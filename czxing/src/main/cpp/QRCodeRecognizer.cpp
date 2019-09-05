//
// Created by Devilsen on 2019/07/14 0014.
//

#include <opencv2/imgproc/types_c.h>
#include "QRCodeRecognizer.h"
#include "opencv2/opencv.hpp"

using namespace cv;
using namespace std;

float getDistance(CvPoint pointO, CvPoint pointA) {
    float distance;
    distance = powf((pointO.x - pointA.x), 2) + powf((pointO.y - pointA.y), 2);
    distance = sqrtf(distance);

    return distance;
}

void check_center(vector<vector<Point> > c, vector<int> &index) {
    float dmin1 = 10000;
    float dmin2 = 10000;
    for (int i = 0; i < c.size(); ++i) {
        RotatedRect rect_i = minAreaRect(c[i]);
        for (int j = i + 1; j < c.size(); ++j) {
            RotatedRect rect_j = minAreaRect(c[j]);
            float d = getDistance(rect_i.center, rect_j.center);
            if (d < dmin2 && d > 10) {
                if (d < dmin1 && d > 10) {
                    dmin2 = dmin1;
                    dmin1 = d;
                    index[2] = index[0];
                    index[3] = index[1];
                    index[0] = i;
                    index[1] = j;

                } else {
                    dmin2 = d;
                    index[2] = i;
                    index[3] = j;
                }
            }
        }
    }
}

void QRCodeRecognizer::processData(const Mat &gray, Rect *resultRect) {

//    Mat gray(h, w, CV_8UC4, data);
//    imwrite("/storage/emulated/0/scan/src.jpg", gray);

//    Mat filter;
//    bilateralFilter(gray, filter, 15, 150, 15, 4);
//    imwrite("/storage/emulated/0/scan/filter.jpg", filter);

    int w = gray.cols;
    int h = gray.rows;

    // 进行canny化，变成黑白线条构成的图片
    Mat binary;
    Canny(gray, binary, 100, 255, 3);
//    imwrite("/storage/emulated/0/scan/src_canny.jpg", binary);
    // detect rectangle now
    vector<vector<Point>> contours;
    vector<Vec4i> hierarchy;
    vector<int> found;
    vector<vector<Point>> found_contours;
    findContours(binary, contours, hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);
//    Mat result = Mat::zeros(gray.size(), CV_8UC4);
    for (int t = 0; t < contours.size(); ++t) {
        double area = contourArea(contours[t]);
        if (area < 150) continue;

        RotatedRect rect = minAreaRect(contours[t]);
        // 根据矩形特征进行几何分析
        float rect_w = rect.size.width;
        float rect_h = rect.size.height;
        float rate = min(rect_w, rect_h) / max(rect_w, rect_h);
        if (rate > 0.65 && rect_w < (gray.cols >> 2) && rect_h < (gray.rows >> 2)) {
            int k = t;
            int c = 0;
            while (hierarchy[k][2] != -1) {
                k = hierarchy[k][2];
                c = c + 1;
            }
            if (c >= 1) {
                found.push_back(t);
                found_contours.push_back(contours[t]);
//                drawContours(result, contours, static_cast<int>(t), Scalar(255, 0, 0), 2, 8);
            }
        }
    }

//    imwrite("/storage/emulated/0/scan/src_patter_1.jpg", result);

    if (found.size() >= 3) {
        vector<int> indexs(4, -1);
        check_center(found_contours, indexs);
        vector<Point> final;
        for (int i = 0; i < 4; ++i) {
            if (indexs[i] == -1) {
                continue;
            }
            RotatedRect part_rect = minAreaRect(found_contours[indexs[i]]);
            Point2f p[4];
            part_rect.points(p);
            for (auto &j : p) {
                final.push_back(j);
            }
        }

        //region of qr
        Rect ROI = boundingRect(final);
        if (ROI.empty()) {
            return;
        }
        int space = 0;
        if (ROI.width < ROI.height) {
            space = ROI.height - ROI.width;
            ROI = ROI + Size(space, 0);
        } else if (ROI.width > ROI.height) {
            space = ROI.width - ROI.height;
            ROI = ROI + Size(0, space);
        }

        Point left_top = ROI.tl();
        Point right_down = ROI.br();
        if (left_top.x >= 20 || left_top.y >= 20 || right_down.x <= w - 20 ||
            right_down.y <= h - 20) {
            ROI = ROI + Point(-20, -20) + Size(40, 40);
        }

        if (ROI.tl().x > 0 && ROI.tl().y > 0 && ROI.br().x < w && ROI.br().y < h) {
//            rectangle(result, ROI.tl(), ROI.br(), Scalar(0, 0, 255));
//            imwrite("/storage/emulated/0/scan/src_patter_2.jpg", result);
            *resultRect = ROI;
        }
    }
}