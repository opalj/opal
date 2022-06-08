#include <jni.h>
#include <stdio.h>
#include <string.h>
#include "TaintTest.h"
JNIEXPORT int JNICALL
Java_TaintTest_sum(JNIEnv *env, jobject obj, jint a, jint b) {
    return a + b;
}

JNIEXPORT int JNICALL
Java_TaintTest_propagate_1source(JNIEnv *env, jobject obj) {
    return source() + 23;
}

JNIEXPORT int JNICALL
Java_TaintTest_propagate_1sanitize(JNIEnv *env, jobject obj, jint a) {
    return sanitize(a);
}

JNIEXPORT int JNICALL
Java_TaintTest_propagate_1sink(JNIEnv *env, jobject obj, jint a) {
    sink(a);
    return 23;
}

JNIEXPORT int JNICALL
Java_TaintTest_sanitize_1only_1a_1into_1sink(JNIEnv *env, jobject obj, jint a, jint b) {
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

