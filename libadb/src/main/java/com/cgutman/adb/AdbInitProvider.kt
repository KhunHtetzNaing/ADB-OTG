package com.cgutman.adb

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log

internal class AdbInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        try {
            if (context == null) throw Exception("Failed to get context.")
            AdbMobile.initialize(context!!)
        } catch (ex: Exception) {
            Log.i(TAG, "Failed to auto initialize the AdbMobile library", ex)
        }
        return false
    }

    override fun query(
        p0: Uri,
        p1: Array<out String>?,
        p2: String?,
        p3: Array<out String>?,
        p4: String?,
    ): Cursor? {
        return null
    }

    override fun getType(p0: Uri): String? {
        return null
    }

    override fun insert(p0: Uri, p1: ContentValues?): Uri? {
        return null
    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int {
        return 0
    }

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
        return 0
    }

    companion object {
        @JvmStatic
        private val TAG = AdbInitProvider::class.java.simpleName
    }
}