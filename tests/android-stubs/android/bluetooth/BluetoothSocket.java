package android.bluetooth;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
public abstract class BluetoothSocket {
    public abstract void connect() throws IOException;
    public abstract InputStream getInputStream() throws IOException;
    public abstract OutputStream getOutputStream() throws IOException;
    public abstract void close() throws IOException;
}
