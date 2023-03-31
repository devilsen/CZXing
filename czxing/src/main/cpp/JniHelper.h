// 
// Created by dongSen on 3/31/23.
//

#ifndef CZXING_JNIHELPER_H
#define CZXING_JNIHELPER_H

#include <jni.h>
#include "config.h"

CZXING_BEGIN_NAMESPACE()

/**
 * retrieve JNIEnv safely.
 * attach first if current thread has no JNIEnv, detach automatically on thread quit.
 * @return
 */
JNIEnv *getEnv();

namespace CZXingJNIInitializer {
void registerJNI(JNIEnv * env);
void unRegisterJNI(JNIEnv * env);
}

CZXING_END_NAMESPACE()

#endif
