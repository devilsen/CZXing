//
// Created by Devilsen on 2019/07/14 0014.
//

#include <opencv2/imgproc/types_c.h>
#include "OpencvProcessor.h"
#include "opencv2/opencv.hpp"
#include "JNIUtils.h"

using namespace cv;
using namespace std;
DetectionBasedTracker *tracker = 0;

class CascadeDetectorAdapter : public DetectionBasedTracker::IDetector {
public:
    CascadeDetectorAdapter(cv::Ptr<cv::CascadeClassifier> detector) :
            IDetector(),
            Detector(detector) {
    }

    //检测到结果  调用  Mat == Bitmap
    void detect(const cv::Mat &Image, std::vector<cv::Rect> &objects) {
        Detector->detectMultiScale(Image, objects, scaleFactor, minNeighbours, 0, minObjSize,
                                   maxObjSize);
    }

    virtual ~CascadeDetectorAdapter() {}

private:
    CascadeDetectorAdapter() = delete;

    cv::Ptr<cv::CascadeClassifier> Detector;
};


void OpencvProcessor::init(const char *path) {
    // 智能指针
    Ptr<CascadeClassifier> classifier = makePtr<CascadeClassifier>(path);
    // 创建检测器
    Ptr<CascadeDetectorAdapter> mainDetector = makePtr<CascadeDetectorAdapter>(classifier);
    // 跟踪器
    Ptr<CascadeClassifier> classifier1 = makePtr<CascadeClassifier>(path);
    // 创建检测器
    Ptr<CascadeDetectorAdapter> trackingDetector = makePtr<CascadeDetectorAdapter>(classifier1);

    DetectionBasedTracker::Parameters DetectorParams;
    // tracker  含有两个对象 检测器 跟踪器
    tracker = new DetectionBasedTracker(mainDetector, trackingDetector, DetectorParams);
    tracker->run();
}

std::vector<cv::Rect> OpencvProcessor::processData(jbyte *data, jint w, jint h) {
    Mat src(h + h / 2, w, CV_8UC1, data);
    // nv21   rgba
    cvtColor(src, src, COLOR_YUV2RGBA_NV21);

    Mat gray;
    cvtColor(src, gray, COLOR_RGBA2GRAY);
    // 对比度    黑白  轮廓 二值化
    equalizeHist(gray, gray);
    // imwrite("/storage/emulated/0/src5.jpg", gray);
    // 检测结果
    std::vector<Rect> results;
    tracker->process(gray);
    tracker->getObjects(results);

    LOGE("检测到二维码： %d个", results.size());
    if (results.size() == 1) {
        imwrite("/storage/emulated/0/src5.jpg", gray);
    }

    src.release();
    gray.release();
    return results;
}

void OpencvProcessor::processData2(int *data, jint w, jint h, Point *point) {

    Mat src(h, w, CV_8UC4, data);
//    cvtColor(src, src, COLOR_YUV2RGBA_NV21);

    Mat gray, binary;
    cvtColor(src, gray, COLOR_BGR2GRAY);
    Canny(gray, binary, 100 , 255, 3);
    imwrite("/storage/emulated/0/scan/src_gray.jpg", binary);

    equalizeHist(binary, binary);
    imwrite("/storage/emulated/0/scan/src_gray2.jpg", binary);

//    不能加这个
//    blur(gray, binary, Size(3, 3));

//    threshold(gray, binary, 100, 255, THRESH_BINARY | THRESH_OTSU);
    imwrite("/storage/emulated/0/scan/src_binary.jpg", binary);

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