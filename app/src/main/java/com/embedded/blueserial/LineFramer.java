package com.embedded.blueserial;

import java.util.function.Consumer;

/** Splits LF/CR/CRLF across arbitrary reads, discarding an entire oversized frame. */
public final class LineFramer {
    private final int limit;
    private final StringBuilder pending = new StringBuilder();
    private boolean dropping, previousCr;
    public LineFramer(int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        this.limit = limit;
    }
    public void accept(String text, Consumer<String> consumer) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\r' || c == '\n') {
                if (!(c == '\n' && previousCr) && !dropping && pending.length() > 0)
                    consumer.accept(pending.toString());
                pending.setLength(0);
                dropping = false;
                previousCr = c == '\r';
            } else {
                previousCr = false;
                if (!dropping) {
                    if (pending.length() == limit) { pending.setLength(0); dropping = true; }
                    else pending.append(c);
                }
            }
        }
    }
    public void reset() { pending.setLength(0); dropping = false; previousCr = false; }
}
