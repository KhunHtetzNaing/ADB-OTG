package com.mrkid.adblib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by xudong on 2/21/14.
 */
public class TcpChannel implements AdbChannel {

    /**
     * The underlying socket that this class uses to
     * communicate with the target device.
     */
    private Socket socket;

    /**
     * The input stream that this class uses to read from
     * the socket.
     */
    private InputStream inputStream;

    /**
     * The output stream that this class uses to read from
     * the socket.
     */
    OutputStream outputStream;


    @Override
    public AdbProtocol.AdbMessage read() throws IOException {

        ByteBuffer packet = ByteBuffer.allocate(AdbProtocol.ADB_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);

			/* Read the header first */
        int dataRead = 0;
        do {
            int bytesRead = inputStream.read(packet.array(), dataRead, 24 - dataRead);

            if (bytesRead < 0)
                throw new IOException("Stream closed");
            else
                dataRead += bytesRead;
        }
        while (dataRead < AdbProtocol.ADB_HEADER_LENGTH);

			/* Pull out header fields */
        int payloadLength = packet.getInt(12);

        byte[] payload = null;
            /* If there's a payload supplied, read that too */
        if (payloadLength != 0) {
            payload = new byte[payloadLength];

            dataRead = 0;
            do {
                int bytesRead = inputStream.read(payload, dataRead, payloadLength - dataRead);

                if (bytesRead < 0)
                    throw new IOException("Stream closed");
                else
                    dataRead += bytesRead;
            }
            while (dataRead < payloadLength);
        }

        return new AdbProtocol.AdbMessage(packet, payload);

    }

    @Override
    public boolean write(AdbProtocol.AdbMessage message) throws IOException {
        ByteBuffer byteBuffer;

        if (message.getPayloadLength() != 0) {
            byteBuffer = ByteBuffer.allocate(AdbProtocol.ADB_HEADER_LENGTH + message.getPayloadLength()).order(ByteOrder.LITTLE_ENDIAN);
        } else {
            byteBuffer = ByteBuffer.allocate(AdbProtocol.ADB_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        }

        byteBuffer.putInt(message.getCommand());
        byteBuffer.putInt(message.getArg0());
        byteBuffer.putInt(message.getArg1());

        byteBuffer.putInt(message.getPayloadLength());
        byteBuffer.putInt(message.getChecksum());

        byteBuffer.putInt(message.getMagic());

        if (message.getPayload() != null) {
            byteBuffer.put(message.getPayload());
        }


        outputStream.write(byteBuffer.array());
        return true;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    public TcpChannel(Socket socket) {
        try {
            this.socket = socket;
            this.inputStream = socket.getInputStream();
            this.outputStream = socket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
