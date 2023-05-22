#include <jni.h>
#include <string>

void register_receiver_intent(JNIEnv *pEnv);

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_opalnativetest_MainActivity_someNativeMethod(
        JNIEnv *env,
        jobject /* this */,
        jint input) {
    // register Broadcast Receiver Intent from native code
    register_receiver_intent(env);

    // do something with input
    std::string output = "Hello from C++, input was " + std::to_string(input);
    return env->NewStringUTF(output.c_str());
}

void register_receiver_intent(JNIEnv *env) {
    // TODO context-registered Broadcast Receiver from native code
    jclass contextClass = env->FindClass("android/content/Context");
    jmethodID registerReceiverMethod = env->GetMethodID(contextClass, "registerReceiver", "(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;");
}
