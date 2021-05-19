//
// Created by Devilsen on 2021/5/19.
//

#ifndef CZXING_MATUTILS_H
#define CZXING_MATUTILS_H

#include <string>
#include <opencv2/core/mat.hpp>
#include "config.h"

CZXING_BEGIN_NAMESPACE()

static void saveMat(const cv::Mat &mat, const std::string& fileName = "src") {
    std::string filePath =
            "/storage/emulated/0/Android/data/me.devilsen.czxing/cache/" + fileName +
            ".jpg";
    cv::Mat resultMat(mat.rows, mat.cols, CV_8UC1, mat.data);
    bool saveResult = imwrite(filePath, mat);
    if (saveResult) {
        LOGE("save result success filePath = %s", filePath.c_str())
    } else {
        LOGE("save result fail")
    }
}

unsigned int m_FileIndex;

static void saveIncreaseMat(const cv::Mat &mat) {
    std::string fileName = std::to_string(m_FileIndex);
    saveMat(mat, fileName);
    m_FileIndex++;
}


CZXING_END_NAMESPACE()

#endif //CZXING_MATUTILS_H
