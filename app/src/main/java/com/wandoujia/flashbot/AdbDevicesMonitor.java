package com.wandoujia.flashbot;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xudong on 2/7/14.
 */
class AdbDevicesMonitor implements Runnable {
  private final Handler handler;

  private List<String> devices = new ArrayList<String>();

  public AdbDevicesMonitor(Handler handler) {
    this.handler = handler;
  }

  @Override
  public void run() {

    devices.clear();

    Command command = new CommandCapture(IdGenerator.next(), Const.timeout, "adb devices") {
      @Override
      public void commandOutput(int id, String line) {
        super.commandOutput(id, line);
        if (TextUtils.isEmpty(line)) {
          // ignore
        } else if (line.trim().equalsIgnoreCase("List of devices attached")) {
          // ignore
        } else {
          // these is a device
          String[] tokens = line.split("\t");
          if (tokens.length == 2) {
            if (tokens[1].equalsIgnoreCase("unauthorized")) {
              // MessageHolder.message = Message.DEVICE_UNAUTHORIZED;
              // unauthorized
            } else {
              devices.add(tokens[0]);
            }
          }
        }
      }

      @Override
      public void commandTerminated(int id, String reason) {
        super.commandTerminated(id, reason);
        Log.e(Const.TAG, "adb devices fails because " + reason);

        handler.postDelayed(AdbDevicesMonitor.this, 1000);
      }

      @Override
      public void commandCompleted(int id, int exitcode) {
        super.commandCompleted(id, exitcode);
        if (exitcode != 0) {
          Log.e(Const.TAG, "adb devices returns !0");
        }

        if (devices.isEmpty()) {
          MessageHolder.message = Message.DEVICE_NOT_FOUND;
          handler.sendEmptyMessage(Message.DEVICE_NOT_FOUND.ordinal());
        } else {
          if (MessageHolder.message == Message.DEVICE_NOT_FOUND) {
            MessageHolder.message = Message.DEVICE_FOUND;
            handler.sendEmptyMessage(Message.DEVICE_FOUND.ordinal());
          }
        }
        handler.postDelayed(AdbDevicesMonitor.this, 1000);
      }
    };

    try {
      Shell shell = ShellUtils.getRootShell();
      if (shell != null) {
        shell.add(command);
      } else {
        handler.sendEmptyMessage(Message.EXIT.ordinal());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
