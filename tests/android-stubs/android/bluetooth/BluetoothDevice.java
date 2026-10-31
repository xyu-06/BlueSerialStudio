package android.bluetooth;
import java.util.UUID;
public class BluetoothDevice {
    private final BluetoothSocket secure, fallback;
    public BluetoothDevice(BluetoothSocket secure, BluetoothSocket fallback) { this.secure=secure; this.fallback=fallback; }
    public BluetoothSocket createRfcommSocketToServiceRecord(UUID uuid) { return secure; }
    public BluetoothSocket createInsecureRfcommSocketToServiceRecord(UUID uuid) { return fallback; }
    public String getName() { return "test module"; }
    public String getAddress() { return "00:11:22:33:44:55"; }
}
