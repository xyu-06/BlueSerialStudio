import com.embedded.blueserial.LineFramer;
import com.embedded.blueserial.OutboundQueue;
import com.embedded.blueserial.StreamDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class SerialWorkflowSelfTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private static String read(OutboundQueue queue) throws Exception {
        return new String(queue.take().bytes, StandardCharsets.UTF_8);
    }
    public static void main(String[] args) throws Exception {
        OutboundQueue queue = new OutboundQueue();
        queue.offer(bytes("old motion"), 1, true, false);
        queue.offer(bytes("query"), 1, false, false);
        queue.offer(bytes("new motion"), 1, true, false);
        check(read(queue).equals("query"), "coalescing must not starve ordinary commands");
        check(read(queue).equals("new motion"), "only latest pending motion is sent");
        for (int i=0; i<OutboundQueue.MAX_PENDING; i++)
            check(queue.offer(bytes("queued"), 1, false, false), "bounded capacity");
        check(!queue.offer(bytes("overflow"), 1, false, false), "full queue rejects ordinary send");
        check(queue.offer(bytes("stop"), 1, false, true), "stop always replaces a full queue");
        check(read(queue).equals("stop"), "stop discards stale pending commands");
        queue.offer(bytes("motion after stop"), 1, true, false);
        check(read(queue).equals("motion after stop"), "motion must resume after an urgent queue clear");
        queue.offer(bytes("old connection"), 1, true, false);
        queue.clear();
        byte[] source = bytes("new connection");
        queue.offer(source, 2, true, false);
        source[0] = 0;
        OutboundQueue.Packet packet = queue.take();
        check(packet.generation == 2 && Arrays.equals(packet.bytes, bytes("new connection")),
            "reconnect clears stale state and packet bytes are copied");
        queue.offerRepeated(bytes("periodic"), 2);
        queue.offer(bytes("manual"), 2, false, false);
        queue.clearRepeated();
        check(read(queue).equals("manual"), "stop loop clears pending repeated commands only");
        CountDownLatch waiting = new CountDownLatch(1);
        AtomicReference<OutboundQueue.Packet> taken = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            waiting.countDown();
            try { taken.set(queue.take()); } catch (InterruptedException e) { throw new AssertionError(e); }
        });
        worker.start();
        check(waiting.await(1, TimeUnit.SECONDS), "worker started");
        queue.offer(bytes("wake"), 3, true, false);
        worker.join(1000);
        check(!worker.isAlive() && taken.get() != null, "enqueue wakes idle writer");
        queue.close();
        check(queue.take() == null && !queue.offer(bytes("closed"), 3, false, true), "close terminates writer");

        LineFramer framer = new LineFramer(8);
        List<String> lines = new ArrayList<>();
        framer.accept("1,2\r", lines::add);
        framer.accept("\n3", lines::add);
        framer.accept(",4\n5\r6\r\n", lines::add);
        check(lines.equals(List.of("1,2", "3,4", "5", "6")), "CR, LF and split CRLF frames");
        lines.clear();
        framer.accept("1234567890", lines::add);
        framer.accept("99\n42\n", lines::add);
        check(lines.equals(List.of("42")), "oversized frame tail must not become a numeric sample");
        lines.clear();
        framer.accept("12345678\n", lines::add);
        check(lines.equals(List.of("12345678")), "exactly at frame limit is valid");
        lines.clear();
        framer.accept("old", lines::add);
        framer.reset();
        framer.accept("new\n", lines::add);
        check(lines.equals(List.of("new")), "reconnect drops previous partial frame");

        byte[] utf8 = bytes("温度🌡=23.5\r\n");
        for (int cut=0; cut<=utf8.length; cut++) {
            StreamDecoder decoder = new StreamDecoder();
            String decoded = decoder.decode(Arrays.copyOfRange(utf8,0,cut))
                + decoder.decode(Arrays.copyOfRange(utf8,cut,utf8.length));
            check(decoded.equals("温度🌡=23.5\r\n"), "UTF-8 split at byte " + cut);
        }
        StreamDecoder decoder = new StreamDecoder();
        decoder.decode(new byte[]{(byte)0xe6}); decoder.reset();
        check(decoder.decode(bytes("OK")).equals("OK"), "decoder drops old partial codepoint on reconnect");
        System.out.println(checks + " serial workflow checks passed");
    }
}
