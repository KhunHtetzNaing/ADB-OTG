package com.htetznaing.adbotg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.util.Log;

import static com.htetznaing.adbotg.Message.USB_PERMISSION;

public class UsbReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action;
        if (intent!=null && (action = intent.getAction()) !=null && action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)){
            Intent intent1 = new Intent(USB_PERMISSION);
            intent1.putExtra(UsbManager.EXTRA_DEVICE,intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
            Log.d("UsbReceiver","Broadcasting USB_CONNECTED");
            context.sendBroadcast(intent1);
        }
    }
}
