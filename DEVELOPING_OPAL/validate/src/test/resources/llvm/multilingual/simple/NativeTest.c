#include <jni.h>
#include <stdio.h>
#include <string.h>
#include "NativeTest.h"
JNIEXPORT int JNICALL
Java_NativeTest_nativeFunction(JNIEnv *env, jobject obj, jint a, jint b)
{
    return a + b;
}

JNIEXPORT int JNICALL
Java_NativeTest_foo(JNIEnv *env, jobject obj)
{
    return bar() + 23;
}

int
bar()
{
    return 6*7;
}