package io.helidon.jenkins.publisher;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.console.ConsoleNote;
import hudson.console.LineTransformationOutputStream;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import io.helidon.jenkins.publisher.config.HelidonPublisherServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * A console log filter to forward the logs.
 */
@Extension
public class PlainLogFilter extends ConsoleLogFilter implements Serializable {

    private final boolean enabled;
    private final String serverUrl;

    public PlainLogFilter() {
        super();
        enabled = false;
        serverUrl = null;
    }

    public PlainLogFilter(Run run) {
        HelidonPublisherServer publisherServer = HelidonPublisherServer.get(run);
        if (publisherServer != null) {
            serverUrl = publisherServer.getServerUrl();
            enabled = true;
        } else {
            enabled = false;
            serverUrl = null;
        }
    }

    @Override
    public OutputStream decorateLogger(AbstractBuild abstractBuild, OutputStream outputStream)
            throws IOException, InterruptedException {

        return decorateLogger((Run) abstractBuild, outputStream);
    }

    @Override
    public OutputStream decorateLogger(Run build, OutputStream outputStream) throws IOException, InterruptedException {
        HelidonPublisherServer pubServer = HelidonPublisherServer.get(build);
        if (pubServer != null) {
            return new ConsoleOutputStream(outputStream, build.getUrl());
        } else {
            return outputStream;
        }
    }

    /**
     * OutputStream wrapper to intercept job output.
     */
    private final class ConsoleOutputStream extends LineTransformationOutputStream {

        private final OutputStream os;
        private final String prefix;

        ConsoleOutputStream(OutputStream os, String prefix) {
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
}