package com.wandoujia.flashbot;

import android.content.Context;
import android.util.Log;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by xudong on 2/7/14.
 */
public class ShellUtils {
  private static Shell instance;

  public static synchronized Shell getRootShell() {
    try {
      if(instance == null || !instance.isRootShellOpen()) {
        instance = RootTools.getShell(true);
        instance.add(new CommandCapture(IdGenerator.next(), "export HOME=/sdcard/Flashbot/"));
      }

      return instance;
    } catch (IOException e) {
      Log.e(Const.TAG, e.getMessage(), e);
    } catch (TimeoutException e) {
      Log.e(Const.TAG, e.getMessage(), e);
    } catch (RootDeniedException e) {
      Log.e(Const.TAG, e.getMessage(), e);
    }
    return null;
  }


}
