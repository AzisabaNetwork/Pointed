package dev.felnull.pointed.util;

import java.nio.ByteBuffer;
import java.util.UUID;

public class Util {
    public static byte[] uuidToBytes(UUID u) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(u.getMostSignificantBits());
        bb.putLong(u.getLeastSignificantBits());
        return bb.array();
    }
}
