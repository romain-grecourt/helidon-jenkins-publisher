package io.helidon.build.publisher.plugin.config;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.annotation.Nonnull;

import hudson.Extension;
import hudson.util.PersistedList;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Helidon Publisher global configuration that configures a list publisher server usable for folders and projects.
 */
@Extension
public final class HelidonPublisherGlobalConfiguration extends GlobalConfiguration {

    private static final Logger LOGGER = Logger.getLogger(HelidonPublisherGlobalConfiguration.class.getName());

    @Nonnull
    public static HelidonPublisherGlobalConfiguration get() {
        return (HelidonPublisherGlobalConfiguration) Jenkins.get().getDescriptorOrDie(HelidonPublisherGlobalConfiguration.class);
    }

    public List<HelidonPublisherServer> servers = new PersistedList<>(this);

    public HelidonPublisherGlobalConfiguration() {
        load();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "servers: {0}", logServers());
        }
    }

    private String logServers() {
        String s = "[";
        Iterator<HelidonPublisherServer> it = servers.iterator();
        while (it.hasNext()) {
            s += it.next().toString();
            if (it.hasNext()) {
                s += ", ";
            }
        }
        return s + "]";
    }

    @Override
    public String getId() {
        return "HelidonPublisher";
    }

    /**
     * Get the configured servers.
     * @return list of {@link HelidonPublisherServer}.
     */
    public List<HelidonPublisherServer> getServers() {
        return servers;
    }

    @DataBoundSetter
    public void setServers(List<HelidonPublisherServer> servers) {
        this.servers = servers;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        super.configure(req, json);
        save();
        return true;
    }

    @Override
    public String getDisplayName() {
        return Messages.DisplayName();
    }
}