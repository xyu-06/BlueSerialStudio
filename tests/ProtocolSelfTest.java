import com.embedded.blueserial.AtCommandBuilder;
import com.embedded.blueserial.SerialProtocol;
import com.embedded.blueserial.DriveControlMath;

/** JVM-only smoke tests; run without Android SDK or Bluetooth hardware. */
public final class ProtocolSelfTest {
    private static void check(boolean value, String title) {
        if (!value) throw new AssertionError(title);
    }
    private interface InvalidAction { void run(); }
    private static void expectInvalidAt(InvalidAction action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid AT command parameter was accepted");
    }
    private static void expectInvalidHex(String input) {
        try { SerialProtocol.parseHex(input); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid HEX was accepted: " + input);
    }
    public static void main(String[] args) {
        check(SerialProtocol.appendSuffix("LED_ON",0).equals("LED_ON"), "none");
        check(SerialProtocol.appendSuffix("LED_ON",1).equals("LED_ON\n"), "LF");
        check(SerialProtocol.appendSuffix("LED_ON",2).equals("LED_ON\r\n"), "CRLF");
        check(SerialProtocol.appendSuffix("LED_ON",3).equals("LED_ON\r"), "CR");
        check(SerialProtocol.appendSuffix("LED_ON",2).getBytes(java.nio.charset.StandardCharsets.UTF_8).length == 8, "CRLF bytes");
        check(SerialProtocol.twoAxis("J,{x},{y}",-50,90).equals("J,-50,90"), "independent axes");
        check(SerialProtocol.twoAxis("J,{x},{y}",0,90).equals("J,0,90"), "right released");
        check(SerialProtocol.twoAxis("J,{x},{y}",-50,0).equals("J,-50,0"), "left released");
        check(SerialProtocol.twoAxis("J,{x},{y}",0,0).equals("J,0,0"), "both released");
        check(SerialProtocol.twoAxis("x={x};y={y}",25,-75).equals("x=25;y=-75"), "custom format");
        check(DriveControlMath.heldSpeed(0) == 18, "initial button speed");
        check(DriveControlMath.heldSpeed(1000) > DriveControlMath.heldSpeed(300), "press duration increases speed");
        check(DriveControlMath.heldSpeed(1600) == 100, "full speed after hold");
        check(DriveControlMath.heldSpeed(3000) == 100, "speed capped at 100");
        check(DriveControlMath.buttonAxis(false,0,false,0,2000) == 0, "released button zeros speed");
        check(DriveControlMath.buttonAxis(true,1000,false,0,1000) == 18, "forward button output");
        check(DriveControlMath.buttonAxis(false,0,true,1000,2600) == -100, "reverse button output");
        check(DriveControlMath.buttonAxis(true,1000,true,1000,2600) == 0, "opposing buttons cancel");
        int x = DriveControlMath.buttonAxis(true,0,false,0,800);
        int y = DriveControlMath.buttonAxis(true,0,false,0,1100);
        check(SerialProtocol.twoAxis("J,{x},{y}",x,y).equals("J,"+x+","+y), "buttons share joystick wire format");
        check(SerialProtocol.twoAxis("J,{x},{y}",0,0).equals("J,0,0"), "urgent stop uses same protocol");
        check(java.util.Arrays.equals(SerialProtocol.parseHex("AA 55 01"),
            new byte[]{(byte)0xAA, (byte)0x55, 0x01}), "HEX spaced bytes");
        check(java.util.Arrays.equals(SerialProtocol.parseHex("AA5501"),
            new byte[]{(byte)0xAA, (byte)0x55, 0x01}), "HEX continuous bytes");
        check(java.util.Arrays.equals(SerialProtocol.parseHex("0xAA,0x55;0x01"),
            new byte[]{(byte)0xAA, (byte)0x55, 0x01}), "HEX prefixes and separators");
        check(java.util.Arrays.equals(SerialProtocol.parseHex("0xAABB"),
            new byte[]{(byte)0xAA, (byte)0xBB}), "HEX continuous prefixed bytes");
        check(SerialProtocol.parseHex("ff").length == 1, "single HEX byte");
        expectInvalidHex("");
        expectInvalidHex("0");
        expectInvalidHex("AAG1");
        expectInvalidHex("AA 5");
        expectInvalidHex("0xGG");
        check(AtCommandBuilder.setName("BlueCar").equals("AT+NAME=BlueCar"), "HC-05 set name");
        check(AtCommandBuilder.setPin("1234").equals("AT+PSWD=1234"), "HC-05 set PIN");
        check(AtCommandBuilder.setRole(0).equals("AT+ROLE=0"), "HC-05 slave role");
        check(AtCommandBuilder.setRole(1).equals("AT+ROLE=1"), "HC-05 master role");
        check(AtCommandBuilder.setRole(2).equals("AT+ROLE=2"), "HC-05 loop role");
        check(AtCommandBuilder.setUart(115200, 1, 2).equals("AT+UART=115200,1,2"), "HC-05 UART command");
        check(AtCommandBuilder.setUart(9600, 0, 0).equals("AT+UART=9600,0,0"), "HC-05 factory UART command");
        check(AtCommandBuilder.setUart(38400, 0, 0).equals("AT+UART=38400,0,0"), "HC-05 AT+ORGL UART default");
        check(AtCommandBuilder.isSupportedBaudRate(5760), "guide baud list literal 5760");
        check(!AtCommandBuilder.isSupportedBaudRate(57600), "do not silently rewrite 5760");
        check(AtCommandBuilder.withCrlf("AT+NAME?").equals("AT+NAME?\r\n"), "HC-05 CRLF suffix");
        expectInvalidAt(() -> AtCommandBuilder.setName(" "));
        expectInvalidAt(() -> AtCommandBuilder.setName("Bad\nName"));
        expectInvalidAt(() -> AtCommandBuilder.setPin("12,34"));
        expectInvalidAt(() -> AtCommandBuilder.setRole(3));
        expectInvalidAt(() -> AtCommandBuilder.setUart(57600, 0, 0));
        expectInvalidAt(() -> AtCommandBuilder.setUart(9600, 2, 0));
        expectInvalidAt(() -> AtCommandBuilder.setUart(9600, 0, 3));
        expectInvalidAt(() -> AtCommandBuilder.withCrlf("AT\r"));
        System.out.println("48 protocol and AT-command checks passed");
    }
}
