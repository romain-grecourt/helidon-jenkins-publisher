package io.helidon.jenkins.publisher.config;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.servlet.ServletException;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;
import hudson.model.Queue;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.DataBoundConstructor;

public class HelidonPublisherServer extends AbstractDescribableImpl<HelidonPublisherServer> {

    private final String serverUrl;
    private final String credentialsId;

    @DataBoundConstructor
    public HelidonPublisherServer(String serverUrl, String credentialsId) {
        this.credentialsId = credentialsId;
        serverUrl =  Util.fixEmptyAndTrim(serverUrl);
        if (serverUrl == null) {
            throw new AssertionError("URL cannot be empty");
        }
        this.serverUrl = serverUrl;
    }

    static URL toURL(String url) {
        url = Util.fixEmptyAndTrim(url);
        if (url == null) {
            return null;
        }
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getServerUrl() {
        return this.serverUrl;
    }

    @CheckForNull
    private static StandardUsernamePasswordCredentials lookupSystemCredentials(@CheckForNull String credentialsId,
            @CheckForNull URL url) {

        if (credentialsId == null) {
            return null;
        }
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StandardUsernamePasswordCredentials.class,
                        Jenkins.get(),
                        ACL.SYSTEM,
                        URIRequirementBuilder.fromUri(url != null ? url.toExternalForm() : null).build()
                ),
                CredentialsMatchers.withId(credentialsId)
        );
    }

    public static String check(String serverUrl) {
        serverUrl = Util.fixEmptyAndTrim(serverUrl);
        if (get(serverUrl) != null) {
            return serverUrl;
        }
        return null;
    }

    public static HelidonPublisherServer get(String serverUrl) {
        List<HelidonPublisherServer> servers = HelidonPublisherGlobalConfiguration.get().getServers();
        for(HelidonPublisherServer server : servers) {
            if (server.getServerUrl().equals(serverUrl)) {
                return server;
            }
        }
        return null;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<HelidonPublisherServer> {

        @SuppressWarnings("unused") // used by stapler
        public FormValidation doCheckUrl(@QueryParameter String value) throws IOException, ServletException {

            if (Util.fixEmptyAndTrim(value) == null) {
                return FormValidation.ok();
            }
            try {
                new URL(value);
            } catch (MalformedURLException e) {
                return FormValidation.error(String.format("Malformed URL (%s)", value), e);
            }
            return FormValidation.ok();
        }

        @SuppressWarnings("unused") // used by stapler
        @RequirePOST
        public FormValidation doVerifyCredentials(@QueryParameter String serverUrl, @QueryParameter String credentialsId) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            try {
                serverUrl = Util.fixEmpty(serverUrl);
                if (serverUrl == null) {
                    return FormValidation.error("No URL given");
                }
                URL url = new URL(serverUrl);
                if (lookupSystemCredentials(Util.fixEmpty(credentialsId), url) == null) {
                    FormValidation.error("Credentials not found");
                }
            } catch (MalformedURLException e) {
                return FormValidation.error(String.format("Malformed URL (%s)", serverUrl), e);
            }
            return FormValidation.ok("Success");
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context, @QueryParameter String url) {
            AccessControlled _context = (context instanceof AccessControlled ? (AccessControlled) context : Jenkins.get());
            if (context == null || !_context.hasPermission(Jenkins.ADMINISTER)) {
                return new StandardUsernameListBoxModel();
            }
            return new StandardUsernameListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(
                            context instanceof Queue.Task ? ((Queue.Task) context).getDefaultAuthentication() : ACL.SYSTEM,
                            context,
                            StandardUsernameCredentials.class,
                            URIRequirementBuilder.create().withScheme("http://").withScheme("https://").build(),
                            CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class)));
        }

        @Override
        public String getDisplayName() {
            return Messages.Server_DisplayName();
        }
    }
}
