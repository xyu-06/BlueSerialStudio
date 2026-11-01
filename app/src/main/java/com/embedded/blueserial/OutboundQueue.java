package com.embedded.blueserial;

import java.util.ArrayDeque;
import java.util.Arrays;

/** Bounded queue with one replaceable motion state and priority stop packets. */
public final class OutboundQueue {
    public static final int MAX_PENDING = 64;
    public static final class Packet {
        public final byte[] bytes;
        public final int generation;
        private final boolean drive, repeated;
        private Packet(byte[] bytes, int generation, boolean drive, boolean repeated) {
            this.bytes = Arrays.copyOf(bytes, bytes.length);
            this.generation = generation;
            this.drive = drive;
            this.repeated = repeated;
        }
    }
    private final ArrayDeque<Packet> pending = new ArrayDeque<>();
    private boolean closed;

    public synchronized boolean offer(byte[] bytes, int generation, boolean drive, boolean urgent) {
        return offer(bytes, generation, drive, urgent, false);
    }
    public synchronized boolean offerRepeated(byte[] bytes, int generation) {
        return offer(bytes, generation, false, false, true);
    }
    public synchronized void clearRepeated() { pending.removeIf(packet -> packet.repeated); }
    private boolean offer(byte[] bytes, int generation, boolean drive, boolean urgent, boolean repeated) {
        if (closed || bytes.length == 0) return false;
        if (urgent) pending.clear();
        else if (drive) pending.removeIf(packet -> packet.drive);
        if (pending.size() >= MAX_PENDING) return false;
        pending.addLast(new Packet(bytes, generation, drive, repeated));
        notifyAll();
        return true;
    }
    public synchronized Packet take() throws InterruptedException {
        while (pending.isEmpty() && !closed) wait();
        return pending.pollFirst();
    }
    public synchronized void clear() { pending.clear(); }
    public synchronized void close() { closed = true; pending.clear(); notifyAll(); }
}
