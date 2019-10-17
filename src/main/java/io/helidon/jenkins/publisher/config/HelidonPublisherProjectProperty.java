package io.helidon.jenkins.publisher.config;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractItem;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.ListBoxModel;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class HelidonPublisherProjectProperty extends JobProperty<Job<?, ?>> {

    public String serverName;

    @DataBoundConstructor
    public HelidonPublisherProjectProperty(String serverName) {
        this.serverName = Util.fixEmptyAndTrim(serverName);
    }

    public static HelidonPublisherServer getServer(String serverName, AbstractItem owner) {
        List<HelidonPublisherServer> servers = HelidonPublisherGlobalConfiguration.get().getServers();
        if (serverName == null && owner != null) {
            ItemGroup itemGroup = owner.getParent();
            while (itemGroup instanceof AbstractFolder<?>) {
                AbstractFolder<?> folder = (AbstractFolder<?>) itemGroup;
                HelidonPublisherFolderProperty folderProp = folder.getProperties().get(HelidonPublisherFolderProperty.class);
                if (folderProp != null && folderProp.serverName != null) {
                    serverName = folderProp.serverName;
                    break;
                }
                itemGroup = folder.getParent();
            }
        }
        for(HelidonPublisherServer server : servers) {
            if (server.getName().equals(serverName)) {
                return server;
            }
        }
        return null;
    }

    @Nullable
    public HelidonPublisherServer getServer() {
        return getServer(serverName, owner);
    }

    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        public static final String PROJECT_BLOCK_NAME = "helidonProjectPublisher";

        @SuppressWarnings("unused") // used by stapler
        public ListBoxModel doFillServerNameItems(@AncestorInPath AbstractFolder<?> folder) {
            ListBoxModel items = new ListBoxModel();
            for (HelidonPublisherServer server : HelidonPublisherGlobalConfiguration.get().getServers()) {
                items.add(server.getName());
            }
            return items;
        }

        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }

        @Override
        public JobProperty<?> newInstance(@Nonnull StaplerRequest req, JSONObject formData) throws FormException {
            HelidonPublisherProjectProperty tpp = req.bindJSON(
                HelidonPublisherProjectProperty.class,
                formData.getJSONObject(PROJECT_BLOCK_NAME));
            if (tpp == null || tpp.serverName == null) {
                return null;
            }
            return tpp;
        }
    }
}