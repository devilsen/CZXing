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

private:
    JavaVM *javaVM;
    JNIEnv *env;
    jobject jSdkObject;
    jmethodID jmid_on_result;

};


#endif //CZXING_JAVACALLHELPER_H
