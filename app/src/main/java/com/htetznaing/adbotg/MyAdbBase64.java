package com.htetznaing.adbotg;

import android.util.Base64;

import com.cgutman.adb.AdbBase64;

/**
 * Created by xudong on 2/28/14.
 */
class MyAdbBase64 implements AdbBase64 {
    @Override
    public String encodeToString(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }
}
