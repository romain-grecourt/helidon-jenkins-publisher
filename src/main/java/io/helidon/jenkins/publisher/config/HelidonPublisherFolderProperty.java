package io.helidon.jenkins.publisher.config;

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

    private final String serverUrl;
    private final String branchExcludes;

    @DataBoundConstructor
    public HelidonPublisherFolderProperty(String serverUrl, String branchExcludes) {
        this.serverUrl = HelidonPublisherServer.validate(serverUrl);
        this.branchExcludes = branchExcludes;
    }

    /**
     * Get the server URL for this folder.
     * @return String
     */
    @Nullable
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Get the excluded branch names.
     * @return String
     */
    public String getBranchExcludes() {
        return branchExcludes;
    }

    @Extension
    public static final class DescriptorImpl extends AbstractFolderPropertyDescriptor {

        public static final String FOLDER_BLOCK_NAME = "helidonFolderPublisher";

        @SuppressWarnings("unused") // used by stapler
        public ListBoxModel doFillServerUrlItems(@AncestorInPath AbstractFolder<?> folder) {
            ListBoxModel items = new ListBoxModel();
            for (HelidonPublisherServer server : HelidonPublisherGlobalConfiguration.get().getServers()) {
                items.add(server.getServerUrl());
            }
            return items;
        }

        @SuppressWarnings("unused") // used by stapler
        public ListBoxModel doFillBranchExcludesItems(@AncestorInPath AbstractFolder<?> folder) {
            return new ListBoxModel();
        }

        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }

        @Override
        public AbstractFolderProperty<?> newInstance(@Nonnull StaplerRequest req, JSONObject formData) throws FormException {
            HelidonPublisherFolderProperty tpp = req.bindJSON(
                HelidonPublisherFolderProperty.class,
                formData.getJSONObject(FOLDER_BLOCK_NAME));
            if (tpp == null || tpp.serverUrl == null) {
                return null;
            }
            return tpp;
        }
    }
}
