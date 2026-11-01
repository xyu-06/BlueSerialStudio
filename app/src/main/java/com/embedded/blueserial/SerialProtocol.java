package com.embedded.blueserial;

/** Stable pure-Java protocol primitives, independent of Bluetooth and Android UI. */
public final class SerialProtocol {
    private SerialProtocol() { }
    public static String suffix(int selection) {
        switch (selection) {
            case 0: return "";
            case 2: return "\r\n";
            case 3: return "\r";
            default: return "\n";
        }
    }
    public static String appendSuffix(String payload, int selection) {
        return payload + suffix(selection);
    }
    /** x is steering (right joystick), y is throttle (left joystick). */
    public static String twoAxis(String format, int steeringX, int throttleY) {
        return format.replace("{x}", Integer.toString(steeringX))
                .replace("{y}", Integer.toString(throttleY));
    }
    /** Parse HEX text into raw bytes. No UTF-8 conversion or line-ending suffix is applied. */
    public static byte[] parseHex(String input) {
        if (input == null || input.trim().isEmpty())
            throw new IllegalArgumentException("不能为空");
        String text = input.trim();
        boolean separated = false;
        for (int i = 0; i < text.length(); i++) separated |= isSeparator(text.charAt(i));
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        int start = 0;
        while (start < text.length()) {
            int end = start;
            while (end < text.length() && !isSeparator(text.charAt(end))) end++;
            int digits = start;
            if (end - digits >= 2 && text.charAt(digits) == '0'
                    && (text.charAt(digits + 1) == 'x' || text.charAt(digits + 1) == 'X')) digits += 2;
            int length = end - digits;
            if (length == 0 || (length & 1) != 0 || (separated && length != 2)) throw invalidHex();
            for (int i = digits; i < end; i += 2) {
                int hi = hexDigit(text.charAt(i)), lo = hexDigit(text.charAt(i + 1));
                if (hi < 0 || lo < 0) throw invalidHex();
                output.write((hi << 4) | lo);
            }
            if (end == text.length()) break;
            while (end < text.length() && isSeparator(text.charAt(end))) end++;
            if (end == text.length()) throw invalidHex();
            start = end;
        }
        return output.toByteArray();
    }
    private static boolean isSeparator(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == 11
            || c == ',' || c == ';' || c == ':' || c == '-';
    }
    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }
    private static IllegalArgumentException invalidHex() {
        return new IllegalArgumentException("请输入完整的十六进制字节，例如 AA 55 01");
    }
}
