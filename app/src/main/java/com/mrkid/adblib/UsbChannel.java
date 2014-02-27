package com.mrkid.adblib;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import com.wandoujia.flashbot.Const;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

/**
 * Created by xudong on 2/21/14.
 */
public class UsbChannel implements AdbChannel {

    private final UsbDeviceConnection mDeviceConnection;
    private final UsbEndpoint mEndpointOut;
    private final UsbEndpoint mEndpointIn;
    private final UsbInterface mInterface;

    // pool of requests for the OUT endpoint
    private final LinkedList<UsbRequest> mOutRequestPool = new LinkedList<UsbRequest>();
    // pool of requests for the IN endpoint
    private final LinkedList<UsbRequest> mInRequestPool = new LinkedList<UsbRequest>();

    // get an OUT request from our pool
    public UsbRequest getOutRequest() {
        synchronized (mOutRequestPool) {
            if (mOutRequestPool.isEmpty()) {
                UsbRequest request = new UsbRequest();
                request.initialize(mDeviceConnection, mEndpointOut);
                return request;
            } else {
                return mOutRequestPool.removeFirst();
            }
        }
    }

    // return an OUT request to the pool
    public void releaseOutRequest(UsbRequest request) {
        synchronized (mOutRequestPool) {
            mOutRequestPool.add(request);
        }
    }

    // return an IN request to the pool
    public void releaseInRequest(UsbRequest request) {
        synchronized (mInRequestPool) {
            mInRequestPool.add(request);
        }
    }


    // get an IN request from the pool
    public UsbRequest getInRequest() {
        synchronized (mInRequestPool) {
            if (mInRequestPool.isEmpty()) {
                UsbRequest request = new UsbRequest();
                request.initialize(mDeviceConnection, mEndpointIn);
                return request;
            } else {
                return mInRequestPool.removeFirst();
            }
        }
    }


        @Override
    public AdbProtocol.AdbMessage read() throws IOException {

        ByteBuffer packet = ByteBuffer.wrap(read(AdbProtocol.ADB_HEADER_LENGTH)).order(ByteOrder.LITTLE_ENDIAN);

        int payloadLength = packet.getInt(12);
        byte[] payload = null;
        if (payloadLength != 0) {
            payload = read(payloadLength);
        }

        return new AdbProtocol.AdbMessage(packet, payload);
    }

    private byte[] read(int length) throws IOException {
        UsbRequest usbRequest = getInRequest();

        ByteBuffer byteBuffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        usbRequest.setClientData(byteBuffer);

        if (!usbRequest.queue(byteBuffer, length)) {
            throw new IOException("fail to queue read UsbRequest");
        }

        while (true) {
            UsbRequest wait = mDeviceConnection.requestWait();

            if (wait == null) {
                throw new IOException("Connection.requestWait return null");
            }

            ByteBuffer clientData = (ByteBuffer) wait.getClientData();
            wait.setClientData(null);

            if (wait.getEndpoint() == mEndpointOut) {
                Log.d(Const.TAG, "a write length " + clientData.capacity() + " complete");
                releaseOutRequest(wait);

            } else if (byteBuffer == clientData) {
                releaseInRequest(wait);
                break;

            } else {
                throw new IOException("unexpected behavior");
            }
        }

        return byteBuffer.array();

    }

    private void writex(byte[] buffer) {

        int offset = 0;
        int transferred = 0;
        while ((transferred = mDeviceConnection.bulkTransfer(mEndpointOut, buffer, offset, buffer.length - offset, 1000)) >= 0) {
            offset += transferred;
            if (offset >= buffer.length) {
                break;
            }
        }
        if (transferred < 0) {
            throw new RuntimeException("bulk transfer fail");
        }

    }

    @Override
    public boolean write(AdbProtocol.AdbMessage message) throws IOException {
//        UsbRequest request = getOutRequest();

        ByteBuffer byteBuffer = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);


        byteBuffer.putInt(message.getCommand());
        byteBuffer.putInt(message.getArg0());
        byteBuffer.putInt(message.getArg1());

        byteBuffer.putInt(message.getPayloadLength());
        byteBuffer.putInt(message.getChecksum());

        byteBuffer.putInt(message.getMagic());

//        request.setClientData(byteBuffer);

        writex(byteBuffer.array());
        if (message.getPayloadLength() > 0) {
            writex(message.getPayload());
        }
        return true;

//        if (request.queue(byteBuffer, 24)) {
//            int length = message.getPayloadLength();
//            if (length > 0) {
//                request = getOutRequest();
//
//                ByteBuffer payloadBuffer = ByteBuffer.wrap(message.getPayload());
//
//                request.setClientData(payloadBuffer);
//                if (request.queue(payloadBuffer, length)) {
//                    return true;
//                } else {
//                    releaseOutRequest(request);
//                    return false;
//                }
//            }
//            return true;
//        } else {
//            releaseOutRequest(request);
//            return false;
//        }
    }

    @Override
    public void close() throws IOException {
        mDeviceConnection.releaseInterface(mInterface);
        mDeviceConnection.close();
    }

    public UsbDeviceConnection getDeviceConnection() {
        return mDeviceConnection;
    }

    public UsbChannel(UsbDeviceConnection connection, UsbInterface intf) {
        mDeviceConnection = connection;
        mInterface = intf;

        UsbEndpoint epOut = null;
        UsbEndpoint epIn = null;
        // look for our bulk endpoints
        for (int i = 0; i < intf.getEndpointCount(); i++) {
            UsbEndpoint ep = intf.getEndpoint(i);
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                    epOut = ep;
                } else {
                    epIn = ep;
                }
            }
        }
        if (epOut == null || epIn == null) {
            throw new IllegalArgumentException("not all endpoints found");
        }
        mEndpointOut = epOut;
        mEndpointIn = epIn;
    }

}
