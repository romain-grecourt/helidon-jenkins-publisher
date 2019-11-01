package io.helidon.jenkins.publisher;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.Run;
import io.helidon.jenkins.publisher.config.HelidonPublisherServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * A console log filter to forward the logs.
 */
@Extension
public class PlainJobLogFilter extends ConsoleLogFilter implements Serializable {

    private final boolean enabled;
    private final String serverUrl;

    public PlainJobLogFilter() {
        super();
        enabled = false;
        serverUrl = null;
    }

    public PlainJobLogFilter(Run run) {
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
    public OutputStream decorateLogger(Run build, OutputStream outputStream) throws IOException, InterruptedException {
        HelidonPublisherServer pubServer = HelidonPublisherServer.get(build);
        if (pubServer != null) {
            return new PlainJobOutputStream(outputStream, build.getUrl());
        } else {
            return outputStream;
        }
    }
}