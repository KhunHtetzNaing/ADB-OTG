package com.mrkid.adblib;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by xudong on 2/21/14.
 */
public interface AdbChannel extends Closeable {
    AdbProtocol.AdbMessage read() throws IOException;

    boolean write(AdbProtocol.AdbMessage message) throws IOException;
}
