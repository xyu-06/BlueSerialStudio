package android.os;
import java.util.ArrayList;
import java.util.List;
public class Handler {
    private static final List<Runnable> ready = new ArrayList<>(), delayed = new ArrayList<>();
    public Handler(Looper looper) { }
    public boolean post(Runnable r) { synchronized(ready) { ready.add(r); } return true; }
    public boolean postDelayed(Runnable r, long delay) { synchronized(delayed) { delayed.add(r); } return true; }
    public void removeCallbacks(Runnable r) {
        synchronized(ready) { ready.removeIf(item -> item == r); }
        synchronized(delayed) { delayed.removeIf(item -> item == r); }
    }
    public void removeCallbacksAndMessages(Object token) {
        synchronized(ready) { ready.clear(); }
        synchronized(delayed) { delayed.clear(); }
    }
    public static void flush() {
        while (true) {
            Runnable r;
            synchronized(ready) { if (ready.isEmpty()) return; r = ready.remove(0); }
            r.run();
        }
    }
    public static void fireTimeouts() {
        List<Runnable> copy;
        synchronized(delayed) { copy = new ArrayList<>(delayed); delayed.clear(); }
        copy.forEach(Runnable::run);
    }
}
