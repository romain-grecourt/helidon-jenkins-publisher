package io.helidon.build.publisher.plugin.config;

import javax.annotation.Nonnull;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import hudson.Extension;
import hudson.util.ListBoxModel;
import javax.annotation.Nullable;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Helidon Publisher folder property that enables publishing a folder.
 */
public final class HelidonPublisherFolderProperty extends AbstractFolderProperty<AbstractFolder<?>> {

    private final HelidonPublisherServer server;
    private final String branchExcludes;
    private final boolean excludeMetaSteps;
    private final boolean excludeSyntheticSteps;

    @DataBoundConstructor
    public HelidonPublisherFolderProperty(String serverName, String branchExcludes, boolean excludeMetaSteps,
            boolean excludeSyntheticSteps) {

        this.server = HelidonPublisherServer.validate(serverName);
        this.branchExcludes = branchExcludes;
        this.excludeMetaSteps = excludeMetaSteps;
        this.excludeSyntheticSteps = excludeSyntheticSteps;
    }

    /**
     * Get the server config for this folder.
     * @return HelidonPublisherServer
     */
    @Nullable
    public HelidonPublisherServer getServer() {
        return server;
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
     *
     * @return {@code true} if meta steps should be excluded, {@code false} otherwise
     */
    public boolean isExcludeMetaSteps() {
        return excludeMetaSteps;
    }

    /**
     * Should the synthetic steps be excluded.
     *
     * @return {@code true} if synthetic steps should be excluded, {@code false} otherwise
     */
    public boolean isExcludeSyntheticSteps() {
        return excludeSyntheticSteps;
    }

    @Extension
    public static final class DescriptorImpl extends AbstractFolderPropertyDescriptor {

        public static final String FOLDER_BLOCK_NAME = "helidonFolderPublisher";

        @SuppressWarnings("unused") // used by stapler
        public ListBoxModel doFillServerNameItems(@AncestorInPath AbstractFolder<?> folder) {
            HelidonPublisherFolderProperty prop = folder.getProperties().get(HelidonPublisherFolderProperty.class);
            String savedServerName = prop != null ? prop.server.getName() : null;
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

        @SuppressWarnings("unused") // used by stapler
        public ListBoxModel doFillBranchExcludesItems(@AncestorInPath AbstractFolder<?> folder) {
            return new ListBoxModel();
        }

        @Override
        public String getDisplayName() {
            return Messages.displayName();
        }

        @Override
        public AbstractFolderProperty<?> newInstance(@Nonnull StaplerRequest req, JSONObject formData) throws FormException {
            HelidonPublisherFolderProperty tpp = req.bindJSON(
                HelidonPublisherFolderProperty.class,
                formData.getJSONObject(FOLDER_BLOCK_NAME));
            if (tpp == null || tpp.server == null) {
                return null;
            }
            return tpp;
        }
    }
}
