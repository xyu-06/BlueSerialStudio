package com.embedded.blueserial;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

/** Session-scoped Classic Bluetooth SPP client; connect/read/write run off the UI thread. */
public final class BluetoothSppClient {
    public interface Listener {
        void onStatus(String label, boolean connected);
        void onReceived(byte[] data);
        void onSent(int count);
        void onError(String message);
    }
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final long CONNECT_TIMEOUT_MS = 15000;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Object socketLock = new Object();
    private final OutboundQueue outgoing = new OutboundQueue();
    private final Listener listener;
    private volatile int generation;
    private volatile BluetoothSocket socket;
    private volatile boolean connected, connecting, closed;
    private final Thread writer;

    public BluetoothSppClient(Listener listener) {
        this.listener = listener;
        writer = new Thread(() -> {
            try {
                OutboundQueue.Packet packet;
                while ((packet = outgoing.take()) != null) writePacket(packet);
            } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }, "SPP-writer");
        writer.start();
    }
    public boolean isConnected() { return connected; }
    public boolean isConnecting() { return connecting; }

    @SuppressLint("MissingPermission")
    public void connect(BluetoothAdapter adapter, BluetoothDevice device, String uuidText) {
        final UUID uuid;
        try { uuid = uuidText.trim().isEmpty() ? SPP_UUID : UUID.fromString(uuidText.trim()); }
        catch (IllegalArgumentException exception) {
            error("UUID 无效，请检查设置中的服务 UUID", generation); return;
        }
        final int id;
        synchronized (socketLock) {
            if (closed) return;
            disconnectLocked();
            id = generation;
            connecting = true;
        }
        status("连接中…", false, id);
        Runnable timeout = () -> {
            synchronized (socketLock) {
                if (generation != id || !connecting) return;
                disconnectLocked();
                error("连接超时，请检查模块电源、距离和配对状态", generation);
                status("未连接", false, generation);
            }
        };
        ui.postDelayed(timeout, CONNECT_TIMEOUT_MS);
        new Thread(() -> {
            BluetoothSocket candidate = null;
            try {
                try { adapter.cancelDiscovery(); } catch (SecurityException ignored) { }
                candidate = device.createRfcommSocketToServiceRecord(uuid);
                if (!track(candidate, id)) return;
                try { candidate.connect(); }
                catch (IOException firstFailure) {
                    safeClose(candidate);
                    if (id != generation) return;
                    candidate = device.createInsecureRfcommSocketToServiceRecord(uuid);
                    if (!track(candidate, id)) return;
                    candidate.connect();
                }
                InputStream stream = candidate.getInputStream();
                String name = device.getName();
                synchronized (socketLock) {
                    if (id != generation || closed) return;
                    connected = true;
                    connecting = false;
                    status("已连接 · " + (name == null ? device.getAddress() : name), true, id);
                }
                ui.removeCallbacks(timeout);
                byte[] buffer = new byte[2048];
                int count;
                while (generation == id && (count = stream.read(buffer)) >= 0) {
                    if (count > 0) {
                        byte[] bytes = Arrays.copyOf(buffer, count);
                        ui.post(() -> { if (!closed && generation == id) listener.onReceived(bytes); });
                    }
                }
            } catch (IOException | SecurityException exception) {
                error("连接/通信失败：" + exception.getMessage(), id);
            } finally {
                ui.removeCallbacks(timeout);
                safeClose(candidate);
                synchronized (socketLock) {
                    // An old reader must never reset a newer connection or clear its queue.
                    if (generation == id) {
                        socket = null;
                        connected = false;
                        connecting = false;
                        outgoing.clear();
                        status("未连接", false, id);
                    }
                }
            }
        }, "SPP-reader-" + id).start();
    }
    private boolean track(BluetoothSocket candidate, int id) {
        synchronized (socketLock) {
            if (generation != id || closed) { safeClose(candidate); return false; }
            socket = candidate;
            return true;
        }
    }
    /** Clears pending commands; a write already inside the OS cannot be recalled. */
    public boolean sendUrgent(byte[] bytes) { return enqueue(bytes, false, true); }
    public boolean sendDrive(byte[] bytes) { return enqueue(bytes, true, false); }
    public boolean send(byte[] bytes) { return enqueue(bytes, false, false); }
    public boolean sendRepeated(byte[] bytes) {
        synchronized (socketLock) {
            if (closed || !connected) return false;
            boolean accepted = outgoing.offerRepeated(bytes, generation);
            if (!accepted) error("循环发送已停止：发送队列已满", generation);
            return accepted;
        }
    }
    public void cancelRepeated() { outgoing.clearRepeated(); }
    private boolean enqueue(byte[] bytes, boolean drive, boolean urgent) {
        synchronized (socketLock) {
            if (closed || !connected || bytes.length == 0) return false;
            boolean accepted = outgoing.offer(bytes, generation, drive, urgent);
            if (!accepted && !drive) error("发送队列已满，请降低发送频率", generation);
            return accepted;
        }
    }
    private void writePacket(OutboundQueue.Packet packet) {
        BluetoothSocket connection;
        synchronized (socketLock) {
            if (!connected || generation != packet.generation) return;
            connection = socket;
        }
        if (connection == null) return;
        try {
            connection.getOutputStream().write(packet.bytes);
            ui.post(() -> {
                if (!closed && generation == packet.generation) listener.onSent(packet.bytes.length);
            });
        } catch (IOException | SecurityException exception) {
            synchronized (socketLock) {
                if (generation == packet.generation) {
                    disconnectLocked();
                    error("发送失败：" + exception.getMessage(), generation);
                    status("未连接", false, generation);
                }
            }
        }
    }
    public void disconnect() {
        synchronized (socketLock) {
            disconnectLocked();
            status("未连接", false, generation);
        }
    }
    private void disconnectLocked() {
        generation++;
        connected = false;
        connecting = false;
        outgoing.clear();
        safeClose(socket);
        socket = null;
    }
    private static void safeClose(BluetoothSocket connection) {
        if (connection != null) try { connection.close(); } catch (IOException ignored) { }
    }
    private void status(String value, boolean active, int id) {
        ui.post(() -> { if (!closed && generation == id) listener.onStatus(value, active); });
    }
    private void error(String value, int id) {
        ui.post(() -> { if (!closed && generation == id) listener.onError(value); });
    }
    public void shutdown() {
        synchronized (socketLock) { closed = true; disconnectLocked(); }
        outgoing.close();
        writer.interrupt();
        ui.removeCallbacksAndMessages(null);
    }
}
