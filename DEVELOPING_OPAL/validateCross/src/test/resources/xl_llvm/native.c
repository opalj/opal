#include "jni.h"

void* globb;

JNIEXPORT void JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_bidirectional_CallJavaFunctionFromNative_callMyJavaFunctionFromNative(
JNIEnv *env, jobject obj, jobject x) {
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/controlflow/bidirectional/CallJavaFunctionFromNative;");
    jmethodID methodID = (*env)->GetMethodID(env, cls, "myJavaFunction", "(Ljava/lang/Object;)V");

    // Call the Java method with the provided argument
    (*env)->CallVoidMethod(env, obj, methodID, x);
}

JNIEXPORT void JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_bidirectional_CreateJavaInstanceFromNative_createInstanceAndCallMyFunctionFromNative(
JNIEnv *env, jclass clsArg, jobject x) {
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/controlflow/bidirectional/CreateJavaInstanceFromNative;");
    jmethodID constructor = (*env)->GetMethodID(env, cls, "<init>", "()V");
    jobject instance = (*env)->NewObject(env, cls, constructor);
    jmethodID methodID = (*env)->GetMethodID(env, cls, "myJavaFunction", "(Ljava/lang/Object;)V");

    (*env)->CallVoidMethod(env, instance, methodID, x);
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

JNIEXPORT void JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_unidirectional_WriteJavaFieldFromNative_setMyfield(
JNIEnv *env, jobject jThis, jobject x) {
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/stateaccess/unidirectional/WriteJavaFieldFromNative;");
    jfieldID fieldID = (*env)->GetFieldID(env, cls, "myfield", "Ljava/lang/Object;");
    (*env)->SetObjectField(env, jThis, fieldID, x);
}

JNIEXPORT jobject JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_unidirectional_ReadJavaFieldFromNative_getMyfield(
JNIEnv *env, jobject jThis, jobject x) {
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/stateaccess/unidirectional/ReadJavaFieldFromNative;");
    jfieldID fieldID = (*env)->GetFieldID(env, cls, "myfield", "Ljava/lang/Object;");
    return (*env)->GetObjectField(env, jThis, fieldID);
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

