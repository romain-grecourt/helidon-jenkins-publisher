package io.helidon.jenkins.publisher.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Helidon Publisher project property that enables publishing a project.
 */
public final class HelidonPublisherProjectProperty extends JobProperty<Job<?, ?>> {

    private final String serverUrl;

    @DataBoundConstructor
    public HelidonPublisherProjectProperty(String serverUrl) {
        this.serverUrl = HelidonPublisherServer.validate(serverUrl);
    }

    /**
     * Get the server URL for this project.
     *
     * @return String
     */
    @Nullable
    public String getServerUrl() {
        return serverUrl;
    }

    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        public static final String PROJECT_BLOCK_NAME = "helidonProjectPublisher";

        @SuppressWarnings("unused") // used by stapler
        public ListBoxModel doFillServerUrlItems(@AncestorInPath AbstractFolder<?> folder) {
            ListBoxModel items = new ListBoxModel();
            for (HelidonPublisherServer server : HelidonPublisherGlobalConfiguration.get().getServers()) {
                items.add(server.getServerUrl());
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
            if (tpp == null || tpp.serverUrl == null) {
                return null;
            }
            return tpp;
        }
    }
}