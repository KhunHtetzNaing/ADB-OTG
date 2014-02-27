package com.wandoujia.flashbot;

import android.util.Log;

import com.mrkid.adblib.AdbConnection;
import com.mrkid.adblib.AdbProtocol;
import com.mrkid.adblib.AdbStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by xudong on 2/25/14.
 */
public class Push {

    private AdbConnection adbConnection;
    private File local;
    private String remotePath;

    public Push(AdbConnection adbConnection, File local, String remotePath) {
        this.adbConnection = adbConnection;
        this.local = local;
        this.remotePath = remotePath;
    }

    public void execute() throws InterruptedException, IOException {

        AdbStream sync = adbConnection.open("sync:");

        String sendId = "SEND";

        String rf = remotePath;
        String mode = ",33206";

        int length = (rf + mode).length();

        ////////DEBUG
        Log.d(Const.TAG, "before push SENDnnnnn");
        sync.write(ByteUtils.concat(sendId.getBytes(), ByteUtils.intToByteArray(length)));
        Log.d(Const.TAG, "after push SENDnnnnn");

        Log.d(Const.TAG, "before push remote file name");
        sync.write(rf.getBytes());
        Log.d(Const.TAG, "after push remote file name");

        Log.d(Const.TAG, "before push remote mode");
        sync.write(mode.getBytes());
        Log.d(Const.TAG, "after push remote mode");

        byte[] buff = new byte[AdbProtocol.CONNECT_MAXDATA];
        InputStream is = new FileInputStream(local);
        while (true) {

            int read = is.read(buff);
            if (read < 0) {
                break;
            }

            Log.d(Const.TAG, "before push DATAnnn");
            sync.write(ByteUtils.concat("DATA".getBytes(), ByteUtils.intToByteArray(read)));
            Log.d(Const.TAG, "after push DATAnnn");

            if (read == buff.length) {
                sync.write(buff);
            } else {
                byte[] tmp = new byte[read];
                System.arraycopy(buff, 0, tmp, 0, read);
                sync.write(tmp);
            }
        }

        sync.write(ByteUtils.concat("DONE".getBytes(), ByteUtils.intToByteArray((int) System.currentTimeMillis())));

        byte[] res = sync.read();
        Log.d(Const.TAG, new String(res));
        // TODO: test if res contains "OKEY" or "FAIL"

        sync.write(ByteUtils.concat("QUIT".getBytes(), ByteUtils.intToByteArray(0)));

    }
}
