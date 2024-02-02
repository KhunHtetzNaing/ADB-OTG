package com.cgutman.adb

import java.io.File

object Adb {
    private var adbKey = File(AdbMobile.getApplicationContext().filesDir, "adbkey")

    init {
        System.loadLibrary("adb_jni")
    }

    private external fun nativeGenerateKey(file: String): Boolean

    private external fun nativeGetPublicKey(file: String): ByteArray

    private external fun nativeSign(file: String, maxPayload: Int, token: ByteArray): ByteArray

    @JvmStatic
    internal fun generateKey() {
        if (!nativeGenerateKey(adbKey.absolutePath)) {
            throw RuntimeException("Failed to generate adb keys")
        }
    }

    @JvmStatic
    fun getPublicKey() = nativeGetPublicKey(adbKey.absolutePath)

    @JvmStatic
    fun sign(maxPayload: Int, token: ByteArray) =
        nativeSign(adbKey.absolutePath, maxPayload, token.copyOf())

    @JvmStatic
    fun sign(token: ByteArray) = nativeSign(adbKey.absolutePath, -1, token.copyOf())
}