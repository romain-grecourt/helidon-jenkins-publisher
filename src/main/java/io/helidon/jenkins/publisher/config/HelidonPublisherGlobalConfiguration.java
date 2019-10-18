package io.helidon.jenkins.publisher.config;

import java.util.List;
import javax.annotation.Nonnull;

import hudson.Extension;
import hudson.util.PersistedList;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

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

    @Override
    public String getId() {
        return "HelidonPublisher";
    }

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