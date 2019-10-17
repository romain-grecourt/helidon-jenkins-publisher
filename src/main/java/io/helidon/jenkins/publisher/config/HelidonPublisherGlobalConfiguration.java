package io.helidon.jenkins.publisher.config;

import hudson.Extension;
import hudson.util.PersistedList;
import java.util.List;
import javax.annotation.Nonnull;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundSetter;

@Extension
public class HelidonPublisherGlobalConfiguration extends GlobalConfiguration {

    @Nonnull
    public static HelidonPublisherGlobalConfiguration get() {
        return (HelidonPublisherGlobalConfiguration) Jenkins.get().getDescriptorOrDie(HelidonPublisherGlobalConfiguration.class);
    }

    public List<HelidonPublisherServer> servers = new PersistedList<>(this);

    public HelidonPublisherGlobalConfiguration() {
        load();
    }

    public List<HelidonPublisherServer> getServers() {
        return servers;
    }

    @DataBoundSetter
    public void setServers(List<HelidonPublisherServer> servers) {
        this.servers = servers;
    }

    @Override
    public String getDisplayName() {
        return Messages.DisplayName();
    }
}