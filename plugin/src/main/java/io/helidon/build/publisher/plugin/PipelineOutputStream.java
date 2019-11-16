package io.helidon.build.publisher.plugin;

import hudson.console.ConsoleNote;
import hudson.console.LineTransformationOutputStream;
import io.helidon.build.publisher.model.Pipeline;
import io.helidon.build.publisher.model.PipelineEvents;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OutputStream wrapper to intercept step output.
 */
final class PipelineOutputStream extends LineTransformationOutputStream {

    private static final AtomicInteger IDS = new AtomicInteger();
    private final OutputStream out;
    private final String runId;
    private final Pipeline.Step step;
    private final int id;
    private final PipelineEvents.EventListener eventListener;

    /**
     * Create a new instance.
     *
     * @param out the stream to wrap
     * @param step the associated step
     */
    PipelineOutputStream(OutputStream out, String runId, Pipeline.Step step, PipelineEvents.EventListener eventListener) {
        super();
        this.out = out;
        this.runId = runId;
        this.step = step;
        this.id = IDS.incrementAndGet();
        this.eventListener = eventListener;
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
            byte[] data = new byte[len];
            System.arraycopy(bytes, 0, data, 0, len);
            eventListener.onEvent(new PipelineEvents.OutputData(runId, step.id(), data));
        }
        if (out != null) {
            out.write(bytes, 0, len);
        }
    }

    @Override
    public String toString() {
        return PipelineOutputStream.class.getSimpleName() + "{"
                + " id=" + id
                + ", step=" + step.id()
                + "}";
    }
}
