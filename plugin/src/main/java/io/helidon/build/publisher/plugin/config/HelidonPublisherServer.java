package io.helidon.build.publisher.plugin.config;

import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.servlet.ServletException;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Helidon Publisher server configuration.
 */
public final class HelidonPublisherServer extends AbstractDescribableImpl<HelidonPublisherServer> {

    private final String serverUrl;
    private final String credentialsId;
    private final int nthread;

    @DataBoundConstructor
    public HelidonPublisherServer(String serverUrl, String credentialsId, int nThreads) {
        this.credentialsId = credentialsId;
        this.nthread = nThreads > 0 ? nThreads : 5;
        serverUrl =  Util.fixEmptyAndTrim(serverUrl);
        if (serverUrl == null) {
            throw new AssertionError("URL cannot be empty");
        }
        this.serverUrl = serverUrl;
    }

    /**
     * Get the credentials ID for this server.
     * @return String
     */
    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Get the URL for this server.
     * @return String
     */
    public String getServerUrl() {
        return this.serverUrl;
    }

    /**
     * Get the number of threads for the client.
     * @return int
     */
    public int getNthread() {
        return nthread;
    }

    @CheckForNull
    public static String lookupCredentials(@CheckForNull String credentialsId, @CheckForNull String url) {
        if (credentialsId == null) {
            return null;
        }
        FileCredentials credentials = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        FileCredentials.class,
                        Jenkins.get(),
                        ACL.SYSTEM,
                        URIRequirementBuilder.fromUri(url).build()
                ),
                CredentialsMatchers.withId(credentialsId)
        );
        if (credentials == null) {
            return null;
        }
        try {
            InputStream is = credentials.getContent();
            byte[] pkey = new byte[2048];
            int i=0;
            for (; i < 2048; i++) {
                int b = is.read();
                if (b > 0) {
                    pkey[i] = (byte) b;
                } else {
                    break;
                }
            }
            return new String(pkey, StandardCharsets.UTF_8)
                    .substring(0, i)
                    .replaceAll("\\n", "")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "");
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Trims and matches the given input URL.
     *
     * @param serverUrl input
     * @return valid URL or {@code null} if the input is empty or the URL doesn't match a server in the global configuration
     */
    static HelidonPublisherServer validate(String serverUrl) {
        serverUrl = Util.fixEmptyAndTrim(serverUrl);
        return HelidonPublisherServer.get(serverUrl);
    }

    /**
     * Get the configured URL for a given run.
     * @param run the run to introspect
     * @return valid server URL or {@code null} if the project / folder does not a have the property enabled.
     */
    public static HelidonPublisherServer get(Run<?, ?> run) {
        if (run != null) {
            if (run instanceof WorkflowRun) {
                ItemGroup itemGroup = ((WorkflowRun) run).getParent().getParent();
                if (itemGroup instanceof AbstractFolder<?>) {
                    AbstractFolder<?> folder = (AbstractFolder<?>) itemGroup;
                    HelidonPublisherFolderProperty prop = folder.getProperties().get(HelidonPublisherFolderProperty.class);
                    if (prop != null) {
                        return prop.getServer();
                    }
                }
            } else {
                HelidonPublisherProjectProperty prop = run.getParent().getProperty(HelidonPublisherProjectProperty.class);
                if (prop != null) {
                    return prop.getServer();
                }
            }
        }
        return null;
    }

    /**
     * Get an instance of {@link HelidonPublisherServer} that matches the given input URL.
     * @param serverUrl input
     * @return HelidonPublisherServer or {@code null} if the URL doesn't match a server in the global configuration
     */
    static HelidonPublisherServer get(String serverUrl) {
        serverUrl = Util.fixEmptyAndTrim(serverUrl);
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
                String pkey = lookupCredentials(Util.fixEmpty(credentialsId), serverUrl);
                if (pkey == null) {
                    return FormValidation.error("Credentials not found");
                }
                int code = HttpSignatureHelper.test(new URL(serverUrl), pkey);
                if (code == 401) {
                    return FormValidation.error("Unauthorized");
                } else if (code == 404) {
                    return FormValidation.error("Not found");
                } else if (code != 200) {
                    return FormValidation.error("Unexpected response code: " + code);
                }
            } catch (MalformedURLException ex) {
                return FormValidation.error(String.format("Malformed URL (%s)", serverUrl), ex);
            } catch (URISyntaxException | IOException ex) {
                return FormValidation.error(ex.getMessage(), ex);
            }
            return FormValidation.ok("Success");
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context, @QueryParameter String url) {
            AccessControlled _context = (context instanceof AccessControlled ? (AccessControlled) context : Jenkins.get());
            if (context == null || !_context.hasPermission(Jenkins.ADMINISTER)) {
                return new StandardListBoxModel();
            }
            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(
                            context instanceof Queue.Task ? ((Queue.Task) context).getDefaultAuthentication() : ACL.SYSTEM,
                            context,
                            FileCredentials.class,
                            URIRequirementBuilder.create().withScheme("http://").withScheme("https://").build(),
                            CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(FileCredentials.class)));
        }

        @Override
        public String getDisplayName() {
            return Messages.serverDisplayName();
        }
    }

    @Override
    public String toString() {
        return "{"
                + " serverUrl=" + serverUrl
                + ", nthreads=" + nthread
                + ", credentialId=" + (credentialsId == null ? "null" : credentialsId)
                + " }";
    }
}
