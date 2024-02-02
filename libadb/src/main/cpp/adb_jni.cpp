//
// Created by Rohit Verma on 16-01-2024.
//

#include <jni.h>
#include <string>
#include <sys/stat.h>

#include "logging.h"
#include "auth.h"

#define ADB_UNUSED(x) x __attribute__((__unused__))

using namespace adb;

static jboolean Adb_GenerateKey(JNIEnv *env, jobject ADB_UNUSED(obj), jstring java_file) {
    auto temp_file = env->GetStringUTFChars(java_file, nullptr);
    auto file = std::string(temp_file);

    auto ret = JNI_TRUE;

    struct stat buf;
    if (stat(file.c_str(), &buf) == -1) {
        if (!auth::GenerateKey(file)) {
            LOGE("Failed to generate new key");
            ret = JNI_FALSE;
        }
    }

    env->ReleaseStringUTFChars(java_file, temp_file);
    return ret;
}

static jbyteArray Adb_GetPublicKey(JNIEnv *env, jobject ADB_UNUSED(obj), jstring java_file) {
    auto temp_file = env->GetStringUTFChars(java_file, nullptr);
    auto file = std::string(temp_file);
    auto key = auth::GetPublicKey(file);
    env->ReleaseStringUTFChars(java_file, temp_file);

    auto data_size = key.size() + 1;
    auto data = env->NewByteArray(data_size);
    env->SetByteArrayRegion(data, 0, data_size,
                            reinterpret_cast<const jbyte *>(key.c_str()));
    return data;
}

static jbyteArray
Adb_Sign(JNIEnv *env, jobject ADB_UNUSED(obj), jstring java_file, jint java_max_payload,
         jbyteArray java_token) {
    auto temp_file = env->GetStringUTFChars(java_file, nullptr);
    auto file = std::string(temp_file);

    size_t token_size = env->GetArrayLength(java_token);
    char token[token_size];
    env->GetByteArrayRegion(java_token, 0, token_size, reinterpret_cast<jbyte *>(token));

    size_t max_payload =
            java_max_payload > 0 ? static_cast<size_t>(java_max_payload) : MAX_PAYLOAD;
    auto signed_token = auth::Sign(file, max_payload, token, token_size);

    env->ReleaseStringUTFChars(java_file, temp_file);

    auto data_size = signed_token.size();
    auto data = env->NewByteArray(data_size);
    env->SetByteArrayRegion(data, 0, data_size,
                            reinterpret_cast<const jbyte *>(signed_token.data()));
    return data;
}

static const JNINativeMethod methods_Adb[] = {
        {"nativeGenerateKey",  "(Ljava/lang/String;)Z",     reinterpret_cast<void *>(Adb_GenerateKey)},
        {"nativeGetPublicKey", "(Ljava/lang/String;)[B",    reinterpret_cast<void *>(Adb_GetPublicKey)},
        {"nativeSign",         "(Ljava/lang/String;I[B)[B", reinterpret_cast<void *>(Adb_Sign)},
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void ADB_UNUSED(*reserved)) {
    JNIEnv *env = nullptr;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    env->RegisterNatives(env->FindClass("com/cgutman/adb/Adb"), methods_Adb,
                         sizeof(methods_Adb) / sizeof(JNINativeMethod));
    return JNI_VERSION_1_6;
}