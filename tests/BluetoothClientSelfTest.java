import android.bluetooth.*;
import android.os.Handler;
import com.embedded.blueserial.BluetoothSppClient;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Runs the real transport against deterministic socket/Handler doubles; not a radio test. */
public final class BluetoothClientSelfTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++; if (!value) throw new AssertionError(message);
    }
    private static void await(CountDownLatch latch) throws Exception {
        check(latch.await(2, TimeUnit.SECONDS), "timed out waiting for socket event");
    }
    private static final class Socket extends BluetoothSocket {
        final CountDownLatch connecting = new CountDownLatch(1), readStarted = new CountDownLatch(1);
        final CountDownLatch connectGate = new CountDownLatch(1), readGate = new CountDownLatch(1);
        final BlockingQueue<String> writes = new LinkedBlockingQueue<>();
        volatile boolean closed, failConnect, failWrite;
        boolean delayedClose;
        Socket(boolean blockConnect) { if (!blockConnect) connectGate.countDown(); }
        public void connect() throws IOException {
            connecting.countDown(); gate(connectGate);
            if (closed || failConnect) throw new IOException("connect failed");
        }
        public InputStream getInputStream() {
            return new InputStream() {
                public int read() throws IOException { readStarted.countDown(); gate(readGate); return -1; }
                public int read(byte[] bytes) throws IOException { return read(); }
            };
        }
        public OutputStream getOutputStream() {
            return new OutputStream() {
                public void write(int value) { }
                public void write(byte[] bytes) throws IOException {
                    if (closed || failWrite) throw new IOException("write failed");
                    writes.add(new String(bytes, StandardCharsets.UTF_8));
                }
            };
        }
        public void close() { closed=true; connectGate.countDown(); if (!delayedClose) readGate.countDown(); }
        static void gate(CountDownLatch gate) throws IOException {
            try { if (!gate.await(2, TimeUnit.SECONDS)) throw new IOException("test gate timeout"); }
            catch (InterruptedException e) { throw new IOException(e); }
        }
    }
    private static final class Events implements BluetoothSppClient.Listener {
        final List<Boolean> statuses = new ArrayList<>();
        final List<String> errors = new ArrayList<>();
        public void onStatus(String value, boolean connected) { statuses.add(connected); }
        public void onReceived(byte[] data) { }
        public void onSent(int count) { }
        public void onError(String error) { errors.add(error); }
    }
    public static void main(String[] args) throws Exception {
        Events events = new Events();
        BluetoothSppClient client = new BluetoothSppClient(events);
        BluetoothAdapter adapter = new BluetoothAdapter();
        try {
            Socket old = new Socket(false); old.delayedClose = true;
            client.connect(adapter, new BluetoothDevice(old, old), "");
            await(old.readStarted);
            check(client.isConnected(), "first connection established");
            Socket fresh = new Socket(false);
            client.connect(adapter, new BluetoothDevice(fresh, fresh), "");
            await(fresh.readStarted);
            Handler.flush();
            check(events.statuses.equals(List.of(false, true)), "old pending connection callbacks ignored");
            old.readGate.countDown();
            client.sendDrive("J,0,60".getBytes(StandardCharsets.UTF_8));
            check("J,0,60".equals(fresh.writes.poll(2, TimeUnit.SECONDS)), "new session receives motion");
            client.sendUrgent("J,0,0".getBytes(StandardCharsets.UTF_8));
            check("J,0,0".equals(fresh.writes.poll(2, TimeUnit.SECONDS)), "urgent stop written");
            client.sendDrive("J,0,20".getBytes(StandardCharsets.UTF_8));
            check("J,0,20".equals(fresh.writes.poll(2, TimeUnit.SECONDS)), "motion works after stop");
            check(client.isConnected(), "late old reader cannot disconnect new session");
            client.disconnect();
            check(!client.send("late".getBytes(StandardCharsets.UTF_8)), "offline send rejected");
            Socket blocked = new Socket(true);
            client.connect(adapter, new BluetoothDevice(blocked, blocked), "");
            await(blocked.connecting);
            check(client.isConnecting(), "connecting is distinguishable and cancelable");
            Handler.fireTimeouts(); Handler.flush();
            check(!client.isConnecting() && blocked.closed, "timeout closes pending socket");
            check(events.errors.stream().anyMatch(e -> e.contains("超时")), "timeout is reported");
            Socket failed = new Socket(false); failed.failConnect=true;
            Socket fallback = new Socket(false);
            client.connect(adapter, new BluetoothDevice(failed, fallback), "");
            await(fallback.readStarted);
            check(client.isConnected() && failed.closed, "secure failure falls back to insecure RFCOMM");
            client.disconnect();
        } finally { client.shutdown(); Handler.flush(); }
        System.out.println(checks + " Bluetooth transport checks passed");
    }
}
