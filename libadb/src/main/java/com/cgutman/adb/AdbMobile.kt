package com.cgutman.adb

import android.content.Context

import java.lang.ref.WeakReference

internal object AdbMobile {
    private lateinit var applicationContext: WeakReference<Context>

    @JvmStatic
    @Synchronized
    fun initialize(context: Context) {
        applicationContext = WeakReference(context)

        Adb.generateKey()
    }

    @JvmStatic
    fun getApplicationContext(): Context {
        if (applicationContext.get() == null) {
            throw RuntimeException("AdbMobile not initialized.")
        }
        return applicationContext.get()!!
    }
}