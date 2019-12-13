package io.helidon.build.publisher.plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.build.publisher.model.Step;
import io.helidon.build.publisher.model.events.StepOutputDataEvent;

import hudson.console.ConsoleNote;
import hudson.console.LineTransformationOutputStream;

/**
 * OutputStream wrapper to intercept step output.
 */
final class PipelineOutputStream extends LineTransformationOutputStream {

    private static final AtomicInteger IDS = new AtomicInteger();
    private final OutputStream out;
    private final String pipelineId;
    private final Step step;
    private final int id;
    private final BackendClient client;

    /**
     * Create a new instance.
     *
     * @param out the stream to wrap
     * @param step the associated step
     */
    PipelineOutputStream(OutputStream out, String pipelineId, Step step, BackendClient client) {
        super();
        this.out = out;
        this.pipelineId = pipelineId;
        this.step = step;
        this.id = IDS.incrementAndGet();
        this.client = client;
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
            client.onEvent(new StepOutputDataEvent(pipelineId, step.id(), data));
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
