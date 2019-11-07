package io.helidon.jenkins.publisher.plugin;

import hudson.console.ConsoleNote;
import hudson.console.LineTransformationOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream wrapper to intercept job output.
 */
public class PlainJobOutputStream extends LineTransformationOutputStream {

    private final OutputStream os;
    private final String prefix;

    PlainJobOutputStream(OutputStream os, String prefix) {
        super();
        this.os = os;
        this.prefix = prefix;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public void flush() throws IOException {
        super.flush();
    }

    @Override
    protected void eol(byte[] bytes, int len) throws IOException {
        if (ConsoleNote.findPreamble(bytes, 0, len) == -1) {
            System.out.print("[" + prefix + "] ");
            System.out.write(bytes, 0, len);
        }
        os.write(bytes, 0, len);
    }
}
