#include "jni.h"


JNIEXPORT void JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_bidirectional_CallJavaFunctionFromNative_callMyJavaFunctionFromNative(JNIEnv *env, jobject obj, jobject x) {
    // Find the Java class and method
    jclass cls = (*env)->GetObjectClass(env, obj);
    jmethodID methodID = (*env)->GetMethodID(env, cls, "myJavaFunction", "(Ljava/lang/Object;)V");

    // Call the Java method with the provided argument
    (*env)->CallVoidMethod(env, obj, methodID, x);
}
JNIEXPORT jobject JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_unidirectional_NativeIdentityFunction_identity(JNIEnv *env, jclass clazz, jobject x) {
    // Simply return the input object as is
    return x;
}