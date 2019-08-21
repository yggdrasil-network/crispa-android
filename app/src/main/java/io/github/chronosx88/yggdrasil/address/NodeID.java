package io.github.chronosx88.yggdrasil.address;

import androidx.annotation.NonNull;

public class NodeID {
    public static final short NODEID_LENGTH = 64; // SHA512 size
    private byte[] bytes;

    public NodeID(String hex) {
        bytes = Utils.hexToBytes(hex);
    }

    public byte[] getBytes() {
        return bytes;
    }

    @NonNull
    @Override
    public String toString() {
        return Utils.bytesToHex(bytes);
    }

    private static class Utils {
        private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        private static String bytesToHex(byte[] bytes) {
            char[] hexChars = new char[bytes.length * 2];
            for (int j = 0; j < bytes.length; j++) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = HEX_ARRAY[v >>> 4];
                hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
            }
            return new String(hexChars);
        }

        private static byte[] hexToBytes(String s) {
            int len = s.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                        + Character.digit(s.charAt(i+1), 16));
            }
            return data;
        }
    }
}
