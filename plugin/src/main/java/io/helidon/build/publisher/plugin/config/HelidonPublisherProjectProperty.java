package io.helidon.build.publisher.plugin.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    private final String serverName;
    private final String branchExcludes;
    private final boolean excludeMetaSteps;
    private final boolean excludeSyntheticSteps;

    @DataBoundConstructor
    public HelidonPublisherProjectProperty(String serverName, String branchExcludes, boolean excludeMetaSteps,
            boolean excludeSyntheticSteps) {

        this.serverName = serverName;
        this.branchExcludes = branchExcludes;
        this.excludeMetaSteps = excludeMetaSteps;
        this.excludeSyntheticSteps = excludeMetaSteps;
    }

    /**
     * Get the server config for this project.
     *
     * @return HelidonPublisherServer
     */
    @Nullable
    public HelidonPublisherServer server() {
        return HelidonPublisherServer.validate(serverName);
    }

    /**
     * Get the server name.
     * @return String
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Get the excluded branch names.
     * @return String
     */
    public String getBranchExcludes() {
        return branchExcludes;
    }

    /**
     * Should the meta steps be excluded.
     * @return {@code true} if meta steps should be excluded, {@code false} otherwise
     */
    public boolean isExcludeMetaSteps() {
        return excludeMetaSteps;
    }

    /**
     * Should the synthetic steps be excluded.
     * @return {@code true} if synthetic steps should be excluded, {@code false} otherwise
     */
    public boolean isExcludeSyntheticSteps() {
        return excludeSyntheticSteps;
    }

    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        public static final String PROJECT_BLOCK_NAME = "helidonProjectPublisher";

        @SuppressWarnings("unused") // used by stapler
        public ListBoxModel doFillServerNameItems(@AncestorInPath Job<?, ?> job) {
            HelidonPublisherProjectProperty prop = job.getProperty(HelidonPublisherProjectProperty.class);
            String savedServerName = prop != null ? prop.serverName : null;
            ListBoxModel items = new ListBoxModel();
            for (HelidonPublisherServer server : HelidonPublisherGlobalConfiguration.get().getServers()) {
                String serverName = server.getName();
                ListBoxModel.Option option = new ListBoxModel.Option(serverName);
                if (serverName.equals(savedServerName)) {
                    option.selected = true;
                }
                items.add(option);
            }
            return items;
        }

        @Override
        public String getDisplayName() {
            return Messages.displayName();
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