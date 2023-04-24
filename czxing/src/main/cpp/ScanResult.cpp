//
// Created by dongSen on 2023/3/31
//

#include <jni.h>
#include "ScanResult.h"
#include "config.h"

USING_CZXING_NAMESPACE()

static struct CodeResult {
    jclass clazz;
    jmethodID builder;
} j_codeResult;

void ScanResult::init(JNIEnv* env)
{
    jclass resultClass = env->FindClass("me/devilsen/czxing/code/CodeResult");
    j_codeResult = {
            reinterpret_cast<jclass>(env->NewGlobalRef(resultClass)),
            env->GetMethodID(resultClass, "<init>", "(Ljava/lang/String;I[II)V"),
    };
    env->DeleteLocalRef(resultClass);
}

void ScanResult::unInit(JNIEnv* env)
{
    env->DeleteGlobalRef(j_codeResult.clazz);
}

jobjectArray ScanResult::obtainResultArray(JNIEnv* env, int size)
{
    return env->NewObjectArray(size, j_codeResult.clazz, nullptr);
}

jintArray getJavaArray(JNIEnv *env, const czxing::CodeRect& codeRect)
{
    jintArray array = env->NewIntArray(4);
    env->SetIntArrayRegion(array, 0, 1, &codeRect.x);
    env->SetIntArrayRegion(array, 1, 1, &codeRect.y);
    env->SetIntArrayRegion(array, 2, 1, &codeRect.width);
    env->SetIntArrayRegion(array, 3, 1, &codeRect.height);

    return array;
}

inline int UTF82UnicodeOne(const char* utf8, wchar_t& wch)
{
    //首字符的Ascii码大于0xC0才需要向后判断，否则，就肯定是单个ANSI字符了
    unsigned char firstCh = utf8[0];
    if (firstCh >= 0xC0)
    {
        //根据首字符的高位判断这是几个字母的UTF8编码
        int afters, code;
        if ((firstCh & 0xE0) == 0xC0)
        {
            afters = 2;
            code = firstCh & 0x1F;
        }
        else if ((firstCh & 0xF0) == 0xE0)
        {
            afters = 3;
            code = firstCh & 0xF;
        }
        else if ((firstCh & 0xF8) == 0xF0)
        {
            afters = 4;
            code = firstCh & 0x7;
        }
        else if ((firstCh & 0xFC) == 0xF8)
        {
            afters = 5;
            code = firstCh & 0x3;
        }
        else if ((firstCh & 0xFE) == 0xFC)
        {
            afters = 6;
            code = firstCh & 0x1;
        }
        else
        {
            wch = firstCh;
            return 1;
        }

        //知道了字节数量之后，还需要向后检查一下，如果检查失败，就简单的认为此UTF8编码有问题，或者不是UTF8编码，于是当成一个ANSI来返回处理
        for(int k = 1; k < afters; ++ k)
        {
            if ((utf8[k] & 0xC0) != 0x80)
            {
                //判断失败，不符合UTF8编码的规则，直接当成一个ANSI字符返回
                wch = firstCh;
                return 1;
            }

            code <<= 6;
            code |= (unsigned char)utf8[k] & 0x3F;
        }

        wch = code;
        return afters;
    }
    else
    {
        wch = firstCh;
    }

    return 1;
}

int UTF82Unicode(const char* utf8Buf, wchar_t *pUniBuf, int utf8Leng)
{
    int i = 0, count = 0;
    while(i < utf8Leng)
    {
        i += UTF82UnicodeOne(utf8Buf + i, pUniBuf[count]);
        count ++;
    }

    return count;
}

jstring stringTojstring(JNIEnv* env, const std::string& str)
{
    int len = str.length();
    wchar_t *wcs = new wchar_t[len * 2];
    int nRet = UTF82Unicode(str.c_str(), wcs, len);
    jchar* jcs = new jchar[nRet];
    for (int i = 0; i < nRet; i++)
    {
        jcs[i] = (jchar) wcs[i];
    }

    jstring retString = env->NewString(jcs, nRet);
    delete[] wcs;
    delete[] jcs;
    return retString;
}

jobject ScanResult::getJCodeResult(JNIEnv* env)
{
    auto text = stringTojstring(env, m_text);
    auto format = static_cast<int>(m_codeFormat);
    return env->NewObject(j_codeResult.clazz, j_codeResult.builder, text, format, getJavaArray(env, rect()), 0);
}

