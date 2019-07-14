#include <jni.h>

//
// Created by Devilsen on 2019/07/14 0014.
//
#ifndef CZXING_OPENCVPROCESSOR_H
#define CZXING_OPENCVPROCESSOR_H

class OpencvProcessor {
public:
    void init(const char *path);

    void processData(jbyte *data, jint w, jint h, jint cameraId);

    void setSurface(JNIEnv *env,jobject surface);
};

#endif //CZXING_OPENCVPROCESSOR_H
