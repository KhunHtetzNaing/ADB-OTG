package com.wandoujia.flashbot;

import com.mrkid.adblib.AdbConnection;
import com.mrkid.adblib.AdbStream;

import java.io.IOException;

/**
 * Created by xudong on 2/25/14.
 */
public class Install {
    private AdbConnection adbConnection;
    private String remotePath;

    public Install(AdbConnection adbConnection, String remotePath) {
        this.adbConnection = adbConnection;
        this.remotePath = remotePath;
    }

    public void execute() throws IOException, InterruptedException {
        AdbStream stream = adbConnection.open("shell:pm install -r " + remotePath);
        while(!stream.isClosed()) {
            stream.read();
        }
    }
}
