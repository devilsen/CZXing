//
// Created by Devilsen on 2019/07/14 0014.
//

#include "OpencvProcessor.h"
#include "opencv2/opencv.hpp"
#include "JNIUtils.h"
#include <android/native_window_jni.h>

using namespace cv;
ANativeWindow *window = 0;
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

    virtual ~CascadeDetectorAdapter() {
    }

private:
    CascadeDetectorAdapter();

    cv::Ptr<cv::CascadeClassifier> Detector;
};


void OpencvProcessor::init(const char *path) {
    //智能指针
    Ptr<CascadeClassifier> classifier = makePtr<CascadeClassifier>(path);

    //创建检测器    classifier RecyclerView    CascadeDetectorAdapter  适配器
    Ptr<CascadeDetectorAdapter> mainDetector = makePtr<CascadeDetectorAdapter>(classifier);
    //    跟踪器
    Ptr<CascadeClassifier> classifier1 = makePtr<CascadeClassifier>(path);

    //创建检测器    classifier RecyclerView    CascadeDetectorAdapter  适配器
    Ptr<CascadeDetectorAdapter> trackingDetector = makePtr<CascadeDetectorAdapter>(classifier1);

    DetectionBasedTracker::Parameters DetectorParams;
    //    tracker  含有两个对象 检测器 跟踪器
    tracker = new DetectionBasedTracker(mainDetector, trackingDetector, DetectorParams);
    tracker->run();
}

void OpencvProcessor::processData(jbyte *data, jint w, jint h, jint cameraId) {
    Mat src(h + h / 2, w, CV_8UC1, data);
    //    nv21   rgba
    cvtColor(src, src, COLOR_YUV2RGBA_NV21);

//    if (cameraId == 1) {
//        //        前置摄像头
//        rotate(src, src, ROTATE_90_COUNTERCLOCKWISE);
//        //        imwrite("/storage/emulated/0/src2.jpg", src);
//        //1  水平翻转 镜像  0  垂直翻转
//        flip(src, src, 1);
//    } else {
//        //顺时针旋转90度
//        rotate(src, src, ROTATE_90_CLOCKWISE);
//    }
    Mat gray;
    cvtColor(src, gray, COLOR_RGBA2GRAY);
    //    imwrite("/storage/emulated/0/src4.jpg", gray);
    //    对比度    黑白  轮廓 二值化
    equalizeHist(gray, gray);
    //    imwrite("/storage/emulated/0/src5.jpg", gray);
    //    检测结果
    std::vector<Rect> faces;
    tracker->process(gray);
    tracker->getObjects(faces);

    LOGE("检测到二维码： %d个", faces.size());

//    for (Rect face : faces) {
//        rectangle(src, face, Scalar(255, 0, 255));
//        Mat m;
//        src(face).copyTo(m);
//        resize(m, m, Size(24, 24));
//        cvtColor(m, m, COLOR_BGR2GRAY);
//        char p[100];
//        sprintf(p, "/storage/emulated/0/wangyi/%d.jpg",i++);
//        imwrite(p,m);
//    }
    //    Mat   src
    if (window) {
        ANativeWindow_setBuffersGeometry(window, src.cols, src.rows, WINDOW_FORMAT_RGBA_8888);
        ANativeWindow_Buffer window_buffer;
        do {
            //lock失败 直接brek出去
            if (ANativeWindow_lock(window, &window_buffer, 0)) {
                ANativeWindow_release(window);
                window = 0;
                break;
            }
            //src.data ： rgba的数据
            //把src.data 拷贝到 buffer.bits 里去
            // 一行一行的拷贝
            //填充rgb数据给dst_data
            uint8_t *dst_data = static_cast<uint8_t *>(window_buffer.bits);
            //stride : 一行多少个数据 （RGBA） * 4
            int dst_linesize = window_buffer.stride * 4;

            //一行一行拷贝
            for (int i = 0; i < window_buffer.height; ++i) {
                memcpy(dst_data + i * dst_linesize, src.data + i * src.cols * 4, dst_linesize);
            }
            //提交刷新
            ANativeWindow_unlockAndPost(window);
        } while (0);
    }
    src.release();
    gray.release();
}

void OpencvProcessor::setSurface(JNIEnv *env, jobject surface) {
    if (window) {
        ANativeWindow_release(window);
        window = 0;
    }
    window = ANativeWindow_fromSurface(env, surface);
}
