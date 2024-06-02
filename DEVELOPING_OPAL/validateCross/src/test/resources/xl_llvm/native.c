#include "jni.h"

void* globb;


// org/opalj/fpcf/fixtures/xl/llvm/controlflow/intraprocedural/interleaved/CallJavaFunctionFromNative
JNIEXPORT void JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_intraprocedural_interleaved_CallJavaFunctionFromNative_callMyJavaFunctionFromNative(JNIEnv* env, jobject jThis, jobject x) {
    printf("Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_intraprocedural_interleaved_CallJavaFunctionFromNative_callMyJavaFunctionFromNative");
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/controlflow/intraprocedural/interleaved/CallJavaFunctionFromNative;");
    jmethodID methodID = (*env)->GetMethodID(env, cls, "myJavaFunction", "(Ljava/lang/Object;)V");

    // Call the Java method with the provided argument
    (*env)->CallVoidMethod(env, jThis, methodID, x);
}


void llvm_controlflow_interprocedural_interleaved_CallJavaFunctionFromNative_callMyJavaFunctionFromNative(JNIEnv* env, jobject jThis, jobject x){
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/controlflow/interprocedural/interleaved/CallJavaFunctionFromNative;");
    jmethodID methodID = (*env)->GetMethodID(env, cls, "myJavaFunction", "(Ljava/lang/Object;)V");

    // Call the Java method with the provided argument
    (*env)->CallVoidMethod(env, jThis, methodID, x);
}
// org/opalj/fpcf/fixtures/xl/llvm/controlflow/interprocedural/interleaved/CallJavaFunctionFromNative
JNIEXPORT void JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_interprocedural_interleaved_CallJavaFunctionFromNative_callMyJavaFunctionFromNative(JNIEnv* env, jobject jThis, jobject x){
    printf("Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_interprocedural_interleaved_CallJavaFunctionFromNative_callMyJavaFunctionFromNative");
    llvm_controlflow_interprocedural_interleaved_CallJavaFunctionFromNative_callMyJavaFunctionFromNative(env, jThis, x);
}


// org/opalj/fpcf/fixtures/xl/llvm/controlflow/intraprocedural/interleaved/CreateJavaInstanceFromNative
JNIEXPORT void JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_intraprocedural_interleaved_CreateJavaInstanceFromNative_createInstanceAndCallMyFunctionFromNative(JNIEnv* env, jclass jThis, jobject x){
    printf("Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_intraprocedural_interleaved_CreateJavaInstanceFromNative_createInstanceAndCallMyFunctionFromNative");
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/controlflow/intraprocedural/interleaved/CreateJavaInstanceFromNative;");
    jmethodID constructor = (*env)->GetMethodID(env, cls, "<init>", "()V");
    jobject instance = (*env)->NewObject(env, cls, constructor);
    jmethodID methodID = (*env)->GetMethodID(env, cls, "myJavaFunction", "(Ljava/lang/Object;)V");

    (*env)->CallVoidMethod(env, instance, methodID, x);
}

void llvm_controlflow_interprocedural_interleaved_CreateJavaInstanceFromNative_createInstanceAndCallMyFunctionFromNative(JNIEnv* env, jclass jThis, jobject x) {
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/controlflow/interprocedural/interleaved/CreateJavaInstanceFromNative;");
    jmethodID constructor = (*env)->GetMethodID(env, cls, "<init>", "()V");
    jobject instance = (*env)->NewObject(env, cls, constructor);
    jmethodID methodID = (*env)->GetMethodID(env, cls, "myJavaFunction", "(Ljava/lang/Object;)V");

    (*env)->CallVoidMethod(env, instance, methodID, x);
}
// org/opalj/fpcf/fixtures/xl/llvm/controlflow/interprocedural/interleaved/CreateJavaInstanceFromNative
JNIEXPORT void JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_interprocedural_interleaved_CreateJavaInstanceFromNative_createInstanceAndCallMyFunctionFromNative(JNIEnv* env, jclass jThis, jobject x){
    printf("Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_interprocedural_interleaved_CreateJavaInstanceFromNative_createInstanceAndCallMyFunctionFromNative");
    llvm_controlflow_interprocedural_interleaved_CreateJavaInstanceFromNative_createInstanceAndCallMyFunctionFromNative(env, jThis, x);
}

// org/opalj/fpcf/fixtures/xl/llvm/controlflow/intraprocedural/interleaved/CallJavaFunctionFromNativeAndReturn
JNIEXPORT jobject JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_intraprocedural_interleaved_CallJavaFunctionFromNativeAndReturn_callMyJavaFunctionFromNative(JNIEnv* env, jobject jThis, jobject x) {
    printf("Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_intraprocedural_interleaved_CallJavaFunctionFromNativeAndReturn_callMyJavaFunctionFromNative");
    // Find the Java class and method
    //jclass cls = (*env)->GetObjectClass(env, obj);
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/controlflow/intraprocedural/interleaved/CallJavaFunctionFromNativeAndReturn;");
    jmethodID methodID = (*env)->GetMethodID(env, cls, "myReturningJavaFunction", "(Ljava/lang/Object;)Ljava/lang/Object;");

    // Call the Java method with the provided argument
    jobject result = (*env)->CallObjectMethod(env, jThis, methodID, x);
    return result;
}

jobject llvm_controlflow_interprocedural_interleaved_CallJavaFunctionFromNativeAndReturn_callMyJavaFunctionFromNative(JNIEnv* env, jobject jThis, jobject x) {
    printf("llvm_controlflow_interprocedural_interleaved_CallJavaFunctionFromNativeAndReturn_callMyJavaFunctionFromNative");
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/controlflow/interprocedural/interleaved/CallJavaFunctionFromNativeAndReturn;");
    jmethodID methodID = (*env)->GetMethodID(env, cls, "myReturningJavaFunction", "(Ljava/lang/Object;)Ljava/lang/Object;");

    // Call the Java method with the provided argument
    jobject result = (*env)->CallObjectMethod(env, jThis, methodID, x);
    return result;
}
// org/opalj/fpcf/fixtures/xl/llvm/controlflow/interprocedural/interleaved/CallJavaFunctionFromNativeAndReturn
JNIEXPORT jobject JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_interprocedural_interleaved_CallJavaFunctionFromNativeAndReturn_callMyJavaFunctionFromNative(JNIEnv* env, jobject jThis, jobject x) {
    return llvm_controlflow_interprocedural_interleaved_CallJavaFunctionFromNativeAndReturn_callMyJavaFunctionFromNative(env, jThis, x);
}

jobject llvm_controlflow_interprocedural_interleaved_CallJavaFunctionFromNativeAndReturn2_callMyJavaFunctionFromNative(JNIEnv* env, jobject jThis, jobject x) {
    printf("llvm_controlflow_interprocedural_interleaved_CallJavaFunctionFromNativeAndReturn_callMyJavaFunctionFromNative");
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/controlflow/interprocedural/interleaved/CallJavaFunctionFromNativeAndReturn2;");
    jmethodID methodID1 = (*env)->GetMethodID(env, cls, "myReturningJavaFunction1", "(Ljava/lang/Object;)Ljava/lang/Object;");

    // Call the Java method with the provided argument
    jobject result1 = (*env)->CallObjectMethod(env, jThis, methodID1, x);

    jmethodID methodID2 = (*env)->GetMethodID(env, cls, "myReturningJavaFunction2", "(Ljava/lang/Object;)Ljava/lang/Object;");

    // Call the Java method with the provided argument
    jobject result2 = (*env)->CallObjectMethod(env, jThis, methodID2, result1);

    return result2;
}
// org/opalj/fpcf/fixtures/xl/llvm/controlflow/interprocedural/interleaved/CallJavaFunctionFromNativeAndReturn
JNIEXPORT jobject JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_interprocedural_interleaved_CallJavaFunctionFromNativeAndReturn2_callMyJavaFunctionFromNative(JNIEnv* env, jobject jThis, jobject x) {
    return llvm_controlflow_interprocedural_interleaved_CallJavaFunctionFromNativeAndReturn2_callMyJavaFunctionFromNative(env, jThis, x);
}


// org/opalj/fpcf/fixtures/xl/llvm/stateaccess/intraprocedural/unidirectional/CAccessJava/WriteJavaFieldFromNative
JNIEXPORT void JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_intraprocedural_unidirectional_CAccessJava_WriteJavaFieldFromNative_setMyfield(JNIEnv* env, jobject jThis, jobject x) {
    printf("Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_intraprocedural_unidirectional_CAccessJava_WriteJavaFieldFromNative_setMyfield");
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/stateaccess/intraprocedural/unidirectional/CAccessJava/WriteJavaFieldFromNative;");
    jfieldID fieldID = (*env)->GetFieldID(env, cls, "myfield", "Ljava/lang/Object;");
    (*env)->SetObjectField(env, jThis, fieldID, x);
}


void llvm_stateaccess_interprocedural_unidirectional_CAccessJava_WriteJavaFieldFromNative_setMyfield(JNIEnv* env, jobject jThis, jobject x) {
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/stateaccess/interprocedural/unidirectional/CAccessJava/WriteJavaFieldFromNative;");
    jfieldID fieldID = (*env)->GetFieldID(env, cls, "myfield", "Ljava/lang/Object;");
    (*env)->SetObjectField(env, jThis, fieldID, x);
}
// org/opalj/fpcf/fixtures/xl/llvm/stateaccess/interprocedural/unidirectional/CAccessJava/WriteJavaFieldFromNative
JNIEXPORT void JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_interprocedural_unidirectional_CAccessJava_WriteJavaFieldFromNative_setMyfield(JNIEnv* env, jobject jThis, jobject x) {
    printf("Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_interprocedural_unidirectional_CAccessJava_WriteJavaFieldFromNative_setMyfield");
    llvm_stateaccess_interprocedural_unidirectional_CAccessJava_WriteJavaFieldFromNative_setMyfield(env, jThis, x);
}


// org/opalj/fpcf/fixtures/xl/llvm/stateaccess/intraprocedural/unidirectional/CAccessJava/ReadJavaFieldFromNative
JNIEXPORT jobject JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_intraprocedural_unidirectional_CAccessJava_ReadJavaFieldFromNative_getMyfield(JNIEnv* env, jobject jThis){
    printf("Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_intraprocedural_unidirectional_CAccessJava_ReadJavaFieldFromNative_getMyfield");
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/stateaccess/intraprocedural/unidirectional/CAccessJava/ReadJavaFieldFromNative;");
    jfieldID fieldID = (*env)->GetFieldID(env, cls, "myfield", "Ljava/lang/Object;");
    return (*env)->GetObjectField(env, jThis, fieldID);
}
jobject llvm_stateaccess_interprocedural_unidirectional_CAccessJava_ReadJavaFieldFromNative_getMyfield(JNIEnv* env, jobject jThis) {
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/stateaccess/interprocedural/unidirectional/CAccessJava/ReadJavaFieldFromNative;");
    jfieldID fieldID = (*env)->GetFieldID(env, cls, "myfield", "Ljava/lang/Object;");
    return (*env)->GetObjectField(env, jThis, fieldID);
}
// org/opalj/fpcf/fixtures/xl/llvm/stateaccess/interprocedural/unidirectional/CAccessJava/ReadJavaFieldFromNative
JNIEXPORT jobject JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_interprocedural_unidirectional_CAccessJava_ReadJavaFieldFromNative_getMyfield(JNIEnv* env, jobject jThis){
    printf("Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_interprocedural_unidirectional_CAccessJava_ReadJavaFieldFromNative_getMyfield");
    return llvm_stateaccess_interprocedural_unidirectional_CAccessJava_ReadJavaFieldFromNative_getMyfield(env, jThis);
}


// org/opalj/fpcf/fixtures/xl/llvm/controlflow/interprocedural/unidirectional/NativeIdentityFunction
JNIEXPORT jobject JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_interprocedural_unidirectional_NativeIdentityFunction_identity(JNIEnv* env, jclass jThis, jobject x) {
    printf("Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_interprocedural_unidirectional_NativeIdentityFunction_identity");
    // Simply return the input object as is
    return x;
}

void* llvm_controlflow_intraprocedural_unidirectional_NativeIdentityFunction_identity(void* x) {
    return x;
}
// org/opalj/fpcf/fixtures/xl/llvm/controlflow/intraprocedural/unidirectional/NativeIdentityFunction
JNIEXPORT jobject JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_intraprocedural_unidirectional_NativeIdentityFunction_identity(JNIEnv* env, jclass jThis, jobject x) {
    printf("Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_intraprocedural_unidirectional_NativeIdentityFunction_identity");
    void* xx = llvm_controlflow_intraprocedural_unidirectional_NativeIdentityFunction_identity(x);
    return xx;
}

JNIEXPORT jobject JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_interprocedural_cyclic_CyclicRecursion_callDecrementFromNative(JNIEnv* env, jclass jThis, jobject x) {
    jclass cls = (*env)->FindClass(env, "Lorg/opalj/fpcf/fixtures/xl/llvm/controlflow/interprocedural/cyclic/CyclicRecursion;");
    jmethodID methodID = (*env)->GetMethodID(env, cls, "decrement", "(Lorg/opalj/fpcf/fixtures/xl/js/testpts/SimpleContainerClass;)Lorg/opalj/fpcf/fixtures/xl/js/testpts/SimpleContainerClass;");

    // Call the Java method with the provided argument
    jobject result = (*env)->CallObjectMethod(env, jThis, methodID, x);
    return result;
}

jobject myglobal;
JNIEXPORT void JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_intraprocedural_bidirectional_WriteNativeGlobalVariable_setNativeGlobal(JNIEnv* env, jclass jThis, jobject x) {
    printf("Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_intraprocedural_bidirectional_WriteNativeGlobalVariable_setNativeGlobal");
    myglobal = (*env)->NewGlobalRef(env, x);
}
JNIEXPORT jobject JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_intraprocedural_bidirectional_WriteNativeGlobalVariable_getNativeGlobal(JNIEnv* env, jclass jThis) {
    printf("Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_intraprocedural_bidirectional_WriteNativeGlobalVariable_getNativeGlobal");
    return myglobal;
}

jobject myglobal2;
void org_opalj_fpcf_fixtures_xl_llvm_stateaccess_interprocedural_bidirectional_WriteNativeGlobalVariable_setNativeGlobal(jobject x){
    myglobal2 = x;
}
JNIEXPORT void JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_interprocedural_bidirectional_WriteNativeGlobalVariable_setNativeGlobal(JNIEnv* env, jclass jThis, jobject x) {
    printf("Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_interprocedural_bidirectional_WriteNativeGlobalVariable_setNativeGlobal");
    org_opalj_fpcf_fixtures_xl_llvm_stateaccess_interprocedural_bidirectional_WriteNativeGlobalVariable_setNativeGlobal((*env)->NewGlobalRef(env, x));
}

jobject org_opalj_fpcf_fixtures_xl_llvm_stateaccess_interprocedural_bidirectional_WriteNativeGlobalVariable_getNativeGlobal() {
    return myglobal2;
}
JNIEXPORT jobject JNICALL Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_interprocedural_bidirectional_WriteNativeGlobalVariable_getNativeGlobal(JNIEnv* env, jclass jThis) {
    printf("Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_interprocedural_bidirectional_WriteNativeGlobalVariable_getNativeGlobal");
    return org_opalj_fpcf_fixtures_xl_llvm_stateaccess_interprocedural_bidirectional_WriteNativeGlobalVariable_getNativeGlobal();
}

// unused
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