//
// Created by Administrator on 2019/08/10 0010.
//

#ifndef CZXING_JAVACALLHELPER_H
#define CZXING_JAVACALLHELPER_H


#include <jni.h>
#include "Result.h"

class JavaCallHelper {
public:
    JavaCallHelper(JavaVM *_javaVM, JNIEnv *_env, jobject &_jobj);

    ~JavaCallHelper();

    void onResult(const ZXing::Result &result);

    void onFocus();

    void onBrightness(const bool isDark);

private:
    JavaVM *javaVM;
    JNIEnv *env;
    jobject jSdkObject;
    jmethodID jmid_on_result;
    jmethodID jmid_on_focus;
    jmethodID jmid_on_brightness;

};


#endif //CZXING_JAVACALLHELPER_H
