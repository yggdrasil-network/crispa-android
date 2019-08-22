package io.github.chronosx88.yggdrasil.address;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class YggAddress {
    private static final short IPV6_ADDRESS_LENGTH = 16; // represents an IPv6 address in the yggdrasil address range.
    private static final short IPV6_SUBNET_LENGTH = 16; // represents an IPv6 /64 subnet in the yggdrasil subnet range.

    private byte[] addressBytes;
    private InetAddress address;

    public YggAddress(NodeID nodeID) {
        addressBytes = new byte[IPV6_ADDRESS_LENGTH];
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        boolean done = false;
        byte ones = 0;
        byte bits = 0;
        byte nBits = 0;
        for(int idx = 0; idx < 8 * nodeID.getBytes().length; idx++) {
            byte bit = (byte) ((nodeID.getBytes()[idx/8] & (0x80 >> (byte)(idx%8))) >> (byte)(7-(idx%8)));
            if(!done && bit != 0) {
                ones++;
                continue;
            }
            if(!done && bit == 0) {
                done = true;
                continue;
            }
            bits = (byte) ((bits << 1) | bit);
            nBits++;
            if(nBits == 8) {
                nBits = 0;
                temp.write(bits);
            }
        }

        byte[] prefix = getPrefix();
        System.arraycopy(prefix, 0, addressBytes, 0, prefix.length);
        addressBytes[prefix.length] = ones;
        System.arraycopy(temp.toByteArray(), 0, addressBytes, prefix.length+1, addressBytes.length-(prefix.length+1));
        try {
            address = InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public byte[] getAddressBytes() {
        return addressBytes;
    }

    public InetAddress getInetAddress() {
        return address;
    }

    public static byte[] getPrefix() {
        return new byte[]{0x02};
    }
}