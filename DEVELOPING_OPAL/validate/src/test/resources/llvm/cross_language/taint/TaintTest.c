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

JNIEXPORT void JNICALL
Java_TaintTest_propagate_1identity_1to_1sink(JNIEnv *env, jobject obj, jint a) {
    int b = identity(a);
    sink(b);
}

JNIEXPORT void JNICALL
Java_TaintTest_propagate_1zero_1to_1sink(JNIEnv *env, jobject obj, jint a) {
    int b = zero(a);
    sink(b);
}

JNIEXPORT void JNICALL
Java_TaintTest_native_1array_1tainted(JNIEnv *env, jobject obj) {
    int a[2] = {0, 0};
    a[1] = source();
    sink(a[1]);
}

JNIEXPORT void JNICALL
Java_TaintTest_native_1array_1untainted(JNIEnv *env, jobject obj) {
    int a[2] = {0, 0};
    a[0] = source();
    sink(a[1]);
}

JNIEXPORT void JNICALL
Java_TaintTest_propagate_1to_1java_1sink(JNIEnv *env, jobject obj, jint a) {
    jclass klass = (*env)->GetObjectClass(env, obj);
    // https://docs.oracle.com/en/java/javase/13/docs/specs/jni/types.html#type-signatures
    // not documented, but "V" is "void"
    jmethodID java_sink = (*env)->GetMethodID(env, klass, "indirect_sink", "(I)V");
    (*env)->CallVoidMethod(env, obj, java_sink, a);
}

JNIEXPORT int JNICALL
Java_TaintTest_propagate_1from_1java_1source(JNIEnv *env, jobject obj) {
     jclass klass = (*env)->GetObjectClass(env, obj);
     jmethodID java_source = (*env)->GetMethodID(env, klass, "indirect_source", "()I");
     return (*env)->CallIntMethod(env, obj, java_source);
}

JNIEXPORT int JNICALL
Java_TaintTest_propagate_1java_1sanitize(JNIEnv *env, jobject obj, jint a) {
     jclass klass = (*env)->GetObjectClass(env, obj);
     jmethodID java_sanitize = (*env)->GetMethodID(env, klass, "indirect_sanitize", "(I)I");
     return (*env)->CallIntMethod(env, obj, java_sanitize, a);
}

int
identity(int a) {
    return a;
}

int
zero(int a) {
    return 0;
}

int
source() {
    return 6*7;
}

void
sink(int num) {
    printf("native %d\n", num);
}

int
sanitize(int num) {
    return num - 19;
}

