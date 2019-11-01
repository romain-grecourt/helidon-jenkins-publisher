package io.helidon.jenkins.publisher;

import hudson.console.ConsoleNote;
import hudson.console.LineTransformationOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OutputStream wrapper to intercept step output.
 */
final class FlowStepOutputStream extends LineTransformationOutputStream {

    private static final AtomicInteger IDS = new AtomicInteger();
    private final OutputStream out;
    private final FlowStep step;
    private final int id;

    /**
     * Create a new instance.
     *
     * @param out the stream to wrap
     * @param step the associated step
     */
    FlowStepOutputStream(OutputStream out, FlowStep step) {
        super();
        this.out = out;
        this.step = step;
        this.id = IDS.incrementAndGet();
    }

    @Override
    public void flush() throws IOException {
        if (out != null) {
            out.flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (out != null) {
            out.close();
        }
    }

    @Override
    protected void eol(byte[] bytes, int len) throws IOException {
        if (ConsoleNote.findPreamble(bytes, 0, len) == -1) {
            System.out.print("[" + this.toString() + "] ");
            System.out.write(bytes, 0, len);
        }
        if (out != null) {
            out.write(bytes, 0, len);
        }
    }

    @Override
    public String toString() {
        return FlowStepOutputStream.class.getSimpleName() + "{"
                + "id=" + id
                + ", step=" + step.id()
                + "}";
    }
}
