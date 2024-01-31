package com.htetznaing.adbotg;

import android.os.Handler;
import android.util.Log;

import com.cgutman.adb.AdbConnection;
import com.cgutman.adb.AdbStream;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by xudong on 2/25/14.
 */
public class Install {
    private final AdbConnection adbConnection;
    private final String remotePath;
    private final long installTimeAssumption;

    public Install(AdbConnection adbConnection, String remotePath, long installTimeAssumption) {
        this.adbConnection = adbConnection;
        this.remotePath = remotePath;
        this.installTimeAssumption = installTimeAssumption;
    }

    public void execute(final Handler handler) throws IOException, InterruptedException {
        final AtomicBoolean done = new AtomicBoolean(false);
        try {
            AdbStream stream = adbConnection.open("shell:pm install -r " + remotePath);
            // we assume installation will take installTimeAssumption milliseconds.
            new Thread() {
                @Override
                public void run() {
                    int percent = 0;

                    while (!done.get()) {
                        handler.sendMessage(handler.obtainMessage(Message.INSTALLING_PROGRESS, Message.PM_INST_PART, percent));

                        if (percent < 95) {
                            percent += 1;
                            try {
                                Thread.sleep(installTimeAssumption / 100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }.start();

            while (!stream.isClosed()) {
                try {
                    Log.d(Const.TAG, new String(stream.read()));
                } catch (IOException e) {
                    // there must be a Stream Close Exception
                    break;
                }
            }
        } finally {
            done.set(true);
            handler.sendMessage(handler.obtainMessage(Message.INSTALLING_PROGRESS, Message.PM_INST_PART, 100));
        }
    }
}
