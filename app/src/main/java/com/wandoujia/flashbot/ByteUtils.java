package com.wandoujia.flashbot;

/**
 * Created by xudong on 2/22/14.
 */
public class ByteUtils {

    public static byte[] concat(byte[]... arrays) {
        // Determine the length of the result array
        int totalLength = 0;
        for (int i = 0; i < arrays.length; i++) {
            totalLength += arrays[i].length;
        }

        // create the result array
        byte[] result = new byte[totalLength];

        // copy the source arrays into the result array
        int currentIndex = 0;
        for (int i = 0; i < arrays.length; i++) {
            System.arraycopy(arrays[i], 0, result, currentIndex, arrays[i].length);
            currentIndex += arrays[i].length;
        }

        return result;
    }

    // TODO: big/little ending
    public static final byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) value,
                (byte) (value >>> 8),
                (byte) (value >>> 16),
                (byte) (value >>> 24)
        };
    }
}
