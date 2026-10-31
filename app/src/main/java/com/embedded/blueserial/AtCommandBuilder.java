package com.embedded.blueserial;

/** Command builder for the HC-05 compatible single-slave AT command set in the supplied guide. */
public final class AtCommandBuilder {
    private static final int[] BAUD_RATES = {
        2400, 4800, 9600, 19200, 38400, 5760, 115200, 230400, 460800, 921600, 1382400
    };

    private AtCommandBuilder() { }

    public static boolean isSupportedBaudRate(int baud) {
        for (int value : BAUD_RATES) if (value == baud) return true;
        return false;
    }

    public static String setName(String name) {
        String value = clean(name, "蓝牙名称不能为空");
        return "AT+NAME=" + value;
    }

    public static String setPin(String pin) {
        String value = clean(pin, "配对码不能为空");
        if (value.indexOf(',') >= 0)
            throw new IllegalArgumentException("配对码不能包含逗号");
        return "AT+PSWD=" + value;
    }

    public static String setRole(int role) {
        if (role < 0 || role > 2)
            throw new IllegalArgumentException("角色参数只能是 0、1 或 2");
        return "AT+ROLE=" + role;
    }

    public static String setUart(int baud, int stopBits, int parity) {
        if (!isSupportedBaudRate(baud))
            throw new IllegalArgumentException("波特率不在说明书列出的范围内");
        if (stopBits != 0 && stopBits != 1)
            throw new IllegalArgumentException("停止位参数只能是 0 或 1");
        if (parity < 0 || parity > 2)
            throw new IllegalArgumentException("校验位参数只能是 0、1 或 2");
        return "AT+UART=" + baud + "," + stopBits + "," + parity;
    }

    public static String withCrlf(String command) {
        String value = clean(command, "AT 指令不能为空");
        if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0)
            throw new IllegalArgumentException("指令中不能手动包含换行符");
        return value + "\r\n";
    }

    private static String clean(String input, String error) {
        if (input == null)
            throw new IllegalArgumentException(error);
        if (input.indexOf('\r') >= 0 || input.indexOf('\n') >= 0)
            throw new IllegalArgumentException("参数中不能包含换行符");
        String value = input.trim();
        if (value.isEmpty())
            throw new IllegalArgumentException(error);
        return value;
    }
}
