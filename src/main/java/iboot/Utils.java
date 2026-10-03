package iboot;

import ghidra.util.Msg;

public final class Utils {
    private Utils() {
        // To prevent instantiation
    }

    public static long toLittleEndianLong(byte[] bytes, int offset, int size) {
        long result = 0;
        for (int i = 0; i < size; i++) {
            // Make sure 'bytes[offset + i]' is always interpreted as unsigned.
            result += (Byte.toUnsignedLong(bytes[offset + i])) * (1l << (8 * i));
        }
        return result;
    }

    // This is as ugly as sin, but I can't find a better way to do the
    // equivalent of 'printf()'. 'System.out.println()' just goes into a
    // black hole.
    public static void debug_log(String message) {
        Msg.showError(null, null, "Debug Log", message);
    }
}
