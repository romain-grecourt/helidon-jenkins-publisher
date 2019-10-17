package io.helidon.jenkins.publisher.config;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import hudson.Extension;
import hudson.Util;
import hudson.util.ListBoxModel;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class HelidonPublisherFolderProperty extends AbstractFolderProperty<AbstractFolder<?>> {

    public final String serverName;

    @DataBoundConstructor
    public HelidonPublisherFolderProperty(String serverName) {
        this.serverName = Util.fixEmptyAndTrim(serverName);
    }

    @Nullable
    public HelidonPublisherServer getServer() {
        return HelidonPublisherProjectProperty.getServer(serverName, owner);
    }

    @Extension
    public static final class DescriptorImpl extends AbstractFolderPropertyDescriptor {

        public static final String FOLDER_BLOCK_NAME = "helidonFolderPublisher";

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
        public AbstractFolderProperty<?> newInstance(@Nonnull StaplerRequest req, JSONObject formData) throws FormException {
            HelidonPublisherFolderProperty tpp = req.bindJSON(
                HelidonPublisherFolderProperty.class,
                formData.getJSONObject(FOLDER_BLOCK_NAME));
            if (tpp == null || tpp.serverName == null) {
                return null;
            }
            return tpp;
        }
    }
}
