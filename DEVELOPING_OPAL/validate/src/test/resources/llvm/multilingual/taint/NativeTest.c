#include <jni.h>
#include <stdio.h>
#include <string.h>
#include "NativeTest.h"
JNIEXPORT int JNICALL
Java_NativeTest_f1(JNIEnv *env, jobject obj, jint a, jint b) {
    return a + b;
}

JNIEXPORT int JNICALL
Java_NativeTest_f2(JNIEnv *env, jobject obj) {
    return source() + 23;
}

JNIEXPORT int JNICALL
Java_NativeTest_f3(JNIEnv *env, jobject obj, jint a) {
    return sanitize(a);
}

JNIEXPORT int JNICALL
Java_NativeTest_f4(JNIEnv *env, jobject obj, jint a) {
    sink(a);
    return 23;
}

JNIEXPORT int JNICALL
Java_NativeTest_f5(JNIEnv *env, jobject obj, jint a, jint b) {
    a = sanitize(a);
    sink(a + b);
    return b;
}

int
source() {
    return 6*7;
}

void
sink(int num) {}

int
sanitize(int num) {
    return num;
}

