#include "jni.h"

void* globb;

JNIEXPORT void JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_bidirectional_CallJavaFunctionFromNative_callMyJavaFunctionFromNative(
JNIEnv *env, jobject obj, jobject x) {
    // Find the Java class and method
    //jclass cls = (*env)->GetObjectClass(env, obj);
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/controlflow/bidirectional/CallJavaFunctionFromNative;");
    jmethodID methodID = (*env)->GetMethodID(env, cls, "myJavaFunction", "(Ljava/lang/Object;)V");

    // Call the Java method with the provided argument
    (*env)->CallVoidMethod(env, obj, methodID, x);
}

JNIEXPORT jobject JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_bidirectional_CallJavaFunctionFromNativeAndReturn_callMyJavaFunctionFromNativeAndReturn(
JNIEnv *env, jobject obj, jobject x) {
    // Find the Java class and method
    //jclass cls = (*env)->GetObjectClass(env, obj);
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/controlflow/bidirectional/CallJavaFunctionFromNativeAndReturn;");
    jmethodID methodID = (*env)->GetMethodID(env, cls, "myReturningJavaFunction", "(Ljava/lang/Object;)Ljava/lang/Object;");

    // Call the Java method with the provided argument
    jobject result = (*env)->CallObjectMethod(env, obj, methodID, x);
    return result;
}

void* nativeIdentityFunction(void* i) {
    return i;
}

void* returnGlobal() {
    return globb;
}
void* returnGlobalCaller() {
return returnGlobal();
}
jobject returnJobjectNullPtr() {
    return NULL;
}
void* otherNativeIdCaller() {
    void* g = returnGlobal();
    void* h = nativeIdentityFunction(g);
    void* j = nativeIdentityFunction(h);
    return j;
}
void setGlobb1() {
char* trololo = "asdasdas";
globb = trololo;
}

void setGlobb2() {
char* xyx = "XYXYXY";
globb = xyx;
}
JNIEXPORT jobject JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_unidirectional_NativeIdentityFunction_identity(
JNIEnv *env, jclass clazz, jobject x) {
    // Simply return the input object as is
    void* xx = nativeIdentityFunction(x);
    return xx;
}

