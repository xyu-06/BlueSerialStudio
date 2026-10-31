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
        String tokenPattern = "(?i)(?:0x)?[0-9a-f]{2}(?:[\\\\s,;:-]+(?:0x)?[0-9a-f]{2})*";
        String compactPattern = "(?i)(?:0x)?(?:[0-9a-f]{2})+";
        String digits;
        if (text.matches(tokenPattern)) {
            StringBuilder combined = new StringBuilder();
            for (String token : text.split("[\\\\s,;:-]+"))
                combined.append(token.replaceFirst("(?i)^0x", ""));
            digits = combined.toString();
        } else if (text.matches(compactPattern)) {
            digits = text.replaceFirst("(?i)^0x", "");
        } else {
            throw new IllegalArgumentException("请输入完整的十六进制字节，例如 AA 55 01");
        }
        if ((digits.length() & 1) != 0)
            throw new IllegalArgumentException("每个字节必须由两位十六进制数字组成");
        byte[] bytes = new byte[digits.length() / 2];
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = (byte) Integer.parseInt(digits.substring(i * 2, i * 2 + 2), 16);
        return bytes;
    }


}
