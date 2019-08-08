#include <jni.h>
#include <string>
#include "JNIUtils.h"
#include "MultiFormatReader.h"
#include "DecodeHints.h"
#include "Result.h"
#include "QRCodeRecognizer.h"
#include "opencv2/opencv.hpp"
#include "ImageUtil.h"
#include <vector>
#include <opencv2/imgproc/types_c.h>
#include "MultiFormatWriter.h"
#include "BitMatrix.h"

static std::vector<ZXing::BarcodeFormat> GetFormats(JNIEnv *env, jintArray formats) {
    std::vector<ZXing::BarcodeFormat> result;
    jsize len = env->GetArrayLength(formats);
    if (len > 0) {
        std::vector<jint> elems(len);
        env->GetIntArrayRegion(formats, 0, elems.size(), elems.data());
        result.resize(len);
        for (jsize i = 0; i < len; ++i) {
            result[i] = ZXing::BarcodeFormat(elems[i]);
        }
    }
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_me_devilsen_czxing_BarcodeReader_createInstance(JNIEnv *env, jclass type, jintArray formats_) {
    try {
        ZXing::DecodeHints hints;
        if (formats_ != nullptr) {
            hints.setPossibleFormats(GetFormats(env, formats_));
        }
        return reinterpret_cast<jlong>(new ZXing::MultiFormatReader(hints));
    }
    catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    }
    catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }
    return 0;
}
extern "C"
JNIEXPORT void JNICALL
Java_me_devilsen_czxing_BarcodeReader_destroyInstance(JNIEnv *env, jclass type, jlong objPtr) {

    try {
        delete reinterpret_cast<ZXing::MultiFormatReader *>(objPtr);
    }
    catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    }
    catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }
}
extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_BarcodeReader_readBarcode(JNIEnv *env, jclass type, jlong objPtr,
                                                  jobject bitmap, jint left, jint top, jint width,
                                                  jint height, jobjectArray result) {

    try {
        auto reader = reinterpret_cast<ZXing::MultiFormatReader *>(objPtr);
        auto binImage = BinaryBitmapFromJavaBitmap(env, bitmap, left, top, width, height);
        if (!binImage) {
            return -1;
        }
        auto readResult = reader->read(*binImage);
        if (readResult.isValid()) {
            env->SetObjectArrayElement(result, 0, ToJavaString(env, readResult.text()));
            if (!readResult.resultPoints().empty()) {
                env->SetObjectArrayElement(result, 1, ToJavaArray(env, readResult.resultPoints()));
            }
            return static_cast<int>(readResult.format());
        }
    }
    catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    }
    catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }
    return -1;

}

extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_BarcodeReader_readBarcodeByte(JNIEnv *env, jclass type, jlong objPtr,
                                                      jbyteArray bytes_, jint left, jint top,
                                                      jint cropWidth, jint cropHeight,
                                                      jint rowWidth, jint rowHeight,
                                                      jobjectArray result) {
    jbyte *bytes = env->GetByteArrayElements(bytes_, NULL);
    ImageUtil imageUtil;
    if (!imageUtil.checkSize(&left, &top)) {
        return -1;
    }

    try {
        Mat src(rowHeight + rowHeight / 2, rowWidth, CV_8UC1, bytes);
//        imwrite("/storage/emulated/0/scan/src.jpg", src);

        cvtColor(src, src, COLOR_YUV2RGBA_NV21);
//        imwrite("/storage/emulated/0/scan/src2.jpg", src);

        if (left != 0) {
            src = src(Rect(left, top, cropWidth, cropHeight));
        }

        rotate(src, src, ROTATE_90_CLOCKWISE);
//        imwrite("/storage/emulated/0/scan/src3.jpg", src);

        Mat gray;
        cvtColor(src, gray, COLOR_RGBA2GRAY);
//        imwrite("/storage/emulated/0/scan/gray.jpg", gray);

        Mat lightMat;
        gray.convertTo(lightMat, -1, 1.0, -60);
//        imwrite("/storage/emulated/0/scan/lightMat.jpg", lightMat);

//        adaptiveThreshold(lightMat, lightMat, 255, ADAPTIVE_THRESH_MEAN_C,
//                          THRESH_BINARY, 55, 3);
//        threshold(lightMat, lightMat, 0, 255, CV_THRESH_OTSU);

        threshold(lightMat, lightMat, 0, 255, CV_THRESH_OTSU);
        imwrite("/storage/emulated/0/scan/threshold.jpg", lightMat);

        //获取自定义核
//        Mat element = getStructuringElement(MORPH_RECT, Size(5, 5));
        // Point(lightMat.rows / 2, lightMat.cols / 2
//        dilate(lightMat, lightMat, element);
//        imwrite("/storage/emulated/0/scan/dilate.jpg", lightMat);

//        medianBlur(lightMat, lightMat, 1);
//        imwrite("/storage/emulated/0/scan/medianBlur.jpg", lightMat);

        cropHeight = lightMat.rows;
        cropWidth = lightMat.cols;

        auto *pixels = static_cast<unsigned char *>(malloc(
                cropWidth * cropHeight * sizeof(unsigned char)));
        int index = 0;
        for (int i = 0; i < cropHeight; ++i) {
            for (int j = 0; j < cropWidth; ++j) {
                pixels[index++] = lightMat.at<unsigned char>(i, j);
            }
        }

        auto reader = reinterpret_cast<ZXing::MultiFormatReader *>(objPtr);

        // 检查处理结果
        Mat resultMat(cropHeight, cropWidth, CV_8UC1, pixels);
        imwrite("/storage/emulated/0/scan/result.jpg", resultMat);

        auto binImage = BinaryBitmapFromBytesC1(env, pixels, 0, 0, cropWidth, cropHeight);
        auto readResult = reader->read(*binImage);

        if (readResult.isValid()) {
            free(pixels);

            env->SetObjectArrayElement(result, 0, ToJavaString(env, readResult.text()));
            if (readResult.isBlurry()) {
                env->SetObjectArrayElement(result, 1, ToJavaArray(env, readResult.resultPoints()));
            }
            return static_cast<int>(readResult.format());
        } else {

            rotate(lightMat, lightMat, ROTATE_90_CLOCKWISE);

            cropHeight = lightMat.rows;
            cropWidth = lightMat.cols;

            index = 0;
            for (int i = 0; i < cropHeight; ++i) {
                for (int j = 0; j < cropWidth; ++j) {
                    pixels[index++] = lightMat.at<unsigned char>(i, j);
                }
            }
            binImage = BinaryBitmapFromBytesC1(env, pixels, 0, 0, cropWidth, cropHeight);
            readResult = reader->read(*binImage);
            if (readResult.isValid()) {
                free(pixels);

                env->SetObjectArrayElement(result, 0, ToJavaString(env, readResult.text()));
                if (readResult.isBlurry()) {
                    env->SetObjectArrayElement(result, 1,
                                               ToJavaArray(env, readResult.resultPoints()));
                }
                return static_cast<int>(readResult.format());
            }
//            LOGE("ssssssssssssssssssssssss");
//            QRCodeRecognizer opencvProcessor;
//            cv::Rect rect;
//            opencvProcessor.processData(lightMat, cropWidth, cropHeight, &rect);
//            free(pixels);
//            if (!rect.empty()) {
//                env->SetObjectArrayElement(result, 2, reactToJavaArray(env, rect));
//                return 1;
//            }
        }


//        auto reader = reinterpret_cast<ZXing::MultiFormatReader *>(objPtr);

// ----------------------------------------------------------------------
//        int *pixels = static_cast<int *>(malloc(cropWidth * cropHeight * sizeof(int)));
//        imageUtil.convertNV21ToGrayScaleRotate(left, top, cropWidth, cropHeight, rowWidth, bytes,
//                                               pixels);
//
//        // 这里进行了旋转，需要把后面的宽高值进行转换
//        int temp = cropWidth;
//        cropWidth = cropHeight;
//        cropHeight = temp;
// ----------------------------------------------------------------------

//        Mat gray(cropHeight, cropWidth, CV_8UC4, pixels);
//        imwrite("/storage/emulated/0/scan/src.jpg", gray);
//
//        cvtColor(gray, gray, CV_BGR2GRAY);
//        // （一）全局阈值使用THRESH_OTSU大津法
//        Mat thresholdMat;
//        threshold(gray, thresholdMat, 0, 255, THRESH_BINARY | THRESH_OTSU);
//        imwrite("/storage/emulated/0/scan/thresholdMat.jpg", thresholdMat);
//
//        // （二）全局阈值使用THRESH_TRIANGLE（三角形算法）
//        Mat thresholdMat2;
//        threshold(gray, thresholdMat2, 0, 255, THRESH_BINARY | THRESH_TRIANGLE);
//        imwrite("/storage/emulated/0/scan/thresholdMat2.jpg", thresholdMat2);
//
//        //(1)THRESH_BINARY_INV大于阈值的都为0
//        Mat thresholdMat3;
//        threshold(gray, thresholdMat3, 127, 255, THRESH_BINARY_INV);
//        imwrite("/storage/emulated/0/scan/thresholdMat3.jpg", thresholdMat3);
//
//        //（2）THRESH_TRUNC截断大于127的值都为127
//        Mat thresholdMat4;
//        threshold(gray, thresholdMat4, 127, 255, THRESH_TRUNC);
//        imwrite("/storage/emulated/0/scan/thresholdMat4.jpg", thresholdMat4);
//
//        //（2）THRESH_TRUNC截断大于127的值都为127
//        Mat thresholdMat5;
//        threshold(gray, thresholdMat5, 127, 255, THRESH_TOZERO);
//        imwrite("/storage/emulated/0/scan/thresholdMat5.jpg", thresholdMat5);
//
//        //（2）THRESH_TRUNC截断大于127的值都为127
//        Mat adaptiveThresholdMat1;
//        adaptiveThreshold(gray,adaptiveThresholdMat1,255,ADAPTIVE_THRESH_MEAN_C,THRESH_BINARY,25,10);
//        imwrite("/storage/emulated/0/scan/adaptiveThresholdMat1.jpg", adaptiveThresholdMat1);
//
//        //（2）THRESH_TRUNC截断大于127的值都为127
//        Mat adaptiveThresholdMat2;
//        adaptiveThreshold(gray,adaptiveThresholdMat2,255,ADAPTIVE_THRESH_GAUSSIAN_C,THRESH_BINARY,25,10);
//        imwrite("/storage/emulated/0/scan/adaptiveThresholdMat2.jpg", adaptiveThresholdMat2);

// -----------------------------------------------------
//        Mat equalizeHistMat;
//        equalizeHist(gray, equalizeHistMat);
//        imwrite("/storage/emulated/0/scan/equalizeHistMat.jpg", equalizeHistMat);
//        // 亮度、对比度增强
//        Mat contrastandbrightImage = Mat::zeros(equalizeHistMat.size(), equalizeHistMat.type());
//        for (int y = 0; y < equalizeHistMat.rows; y++) {
//            for (int x = 0; x < equalizeHistMat.cols; x++) {
//                contrastandbrightImage.at<uchar>(y, x) = saturate_cast<uchar>(6 * (equalizeHistMat.at<uchar>(y, x)));
//            }
//        }
//
//        Mat thresholdImage;
//        blur(contrastandbrightImage,contrastandbrightImage,Size(3,3));
//        threshold(contrastandbrightImage,thresholdImage, 0, 255, CV_THRESH_OTSU+CV_THRESH_BINARY_INV);
//        Mat element = getStructuringElement(MORPH_RECT, Size(3, 3));
//        //进行膨胀操作
//        Mat dilateImage;
//        dilate(thresholdImage, dilateImage, element);
//        dilate(dilateImage, dilateImage, element);
//        dilate(dilateImage, dilateImage, element);
//        dilate(dilateImage, dilateImage, element);
//        dilate(dilateImage, dilateImage, element);
//        imwrite("/storage/emulated/0/scan/dilateImage.jpg", dilateImage);
// -----------------------------------------------------

//        Mat thresholdImage;
//        threshold(gray, thresholdImage, 100, 255, CV_THRESH_OTSU);
//        blur(thresholdImage, thresholdImage, Size(3, 3));
//
//        imwrite("/storage/emulated/0/scan/thresholdImage.jpg", thresholdImage);
//


// -----------------------------------------------------
//        Mat adaptive;
//        adaptiveThreshold(gray, adaptive, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 25, 10);
//        imwrite("/storage/emulated/0/scan/adaptiveThreshold.jpg", adaptive);

// -------------------------------------------------------
        //        Mat filter;
//        blur(gray, filter,);
//        imwrite("/storage/emulated/0/scan/filter.jpg", filter);

//        int index = 0;
//        for (int i = 0; i < cropWidth / 4; ++i) {
//            for (int j = 0; j < cropHeight / 4; ++j) {
//                pixels[index++] = adaptive.at<int>(i, j);
//            }
//        }
//
//        Mat resultMat(cropHeight, cropWidth, CV_8UC4, pixels);
//        imwrite("/storage/emulated/0/scan/result.jpg", resultMat);
//
//        auto binImage = BinaryBitmapFromBytes(env, pixels, 0, 0, cropWidth, cropHeight);
//        auto readResult = reader->read(*binImage);

//        if (readResult.isValid()) {
//            free(pixels);
//
//            env->SetObjectArrayElement(result, 0, ToJavaString(env, readResult.text()));
//            env->SetObjectArrayElement(result, 1, ToJavaArray(env, readResult.resultPoints()));
//            return static_cast<int>(readResult.format());
//        } else {
//            QRCodeRecognizer opencvProcessor;
//            cv::Rect rect;
//            opencvProcessor.processData(pixels, cropWidth, cropHeight, &rect);
//            free(pixels);
//            if (!rect.empty()) {
//                env->SetObjectArrayElement(result, 2, reactToJavaArray(env, rect));
//                return 1;
//            }
//        }
    }
    catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    }
    catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }
    env->ReleaseByteArrayElements(bytes_, bytes, 0);

    return -1;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_devilsen_czxing_BarcodeReader_analysisBrightnessNative(JNIEnv *env, jclass type,
                                                               jbyteArray bytes_, jint width,
                                                               jint height) {
    jbyte *bytes = env->GetByteArrayElements(bytes_, NULL);

    bool isDark = AnalysisBrightness(env, bytes, width, height);
    env->ReleaseByteArrayElements(bytes_, bytes, 0);

    return isDark ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_devilsen_czxing_BarcodeWriter_writeCode(JNIEnv *env, jclass type, jstring content_,
                                                jint width, jint height, jint color,
                                                jstring format_, jobjectArray result) {
    const char *content = env->GetStringUTFChars(content_, 0);
    const char *format = env->GetStringUTFChars(format_, 0);
    try {
        std::wstring wContent;
        wContent = ANSIToUnicode(content);

        ZXing::MultiFormatWriter writer(ZXing::BarcodeFormatFromString(format));
        ZXing::BitMatrix bitMatrix = writer.encode(wContent, width, height);

        if (bitMatrix.empty()) {
            return -1;
        }

        int size = width * height;
        jintArray pixels = env->NewIntArray(size);
        int black = color;
        int white = 0xffffffff;
        int index = 0;
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                int pix = bitMatrix.get(j, i) ? black : white;
                env->SetIntArrayRegion(pixels, index, 1, &pix);
                index++;
            }
        }
        env->SetObjectArrayElement(result, 0, pixels);
        env->ReleaseStringUTFChars(format_, format);
        env->ReleaseStringUTFChars(content_, content);
    }
    catch (const std::exception &e) {
        ThrowJavaException(env, e.what());
    }
    catch (...) {
        ThrowJavaException(env, "Unknown exception");
    }
    return 0;
}