package io.helidon.build.publisher.plugin;

import hudson.model.Actionable;
import hudson.model.Run;
import io.helidon.build.publisher.plugin.config.HelidonPublisherFolderProperty;
import io.helidon.build.publisher.plugin.config.HelidonPublisherProjectProperty;
import io.helidon.build.publisher.plugin.config.HelidonPublisherServer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

/**
 * Pipeline run info.
 */
final class PipelineRunInfo {

    private static final char[] HEX_CODE = "0123456789ABCDEF".toCharArray();

    final String id;
    final boolean excludeSyntheticSteps;
    final boolean excludeMetaSteps;
    final String jobName;
    final String scmHead;
    final String scmHash;
    final String publisherServerUrl;
    final int publisherClientThreads;

    PipelineRunInfo() {
        this.id = null;
        this.excludeSyntheticSteps = false;
        this.excludeMetaSteps = false;
        this.publisherClientThreads = 0;
        this.jobName = null;
        this.scmHead = null;
        this.scmHash = null;
        this.publisherServerUrl = null;
    }

    PipelineRunInfo(FlowExecution execution) {
        WorkflowRun run = Helper.getRun(execution.getOwner());
        WorkflowMultiBranchProject project = Helper.getProject(run.getParent());
        jobName = project.getName();
        HelidonPublisherFolderProperty prop = project.getProperties().get(HelidonPublisherFolderProperty.class);
        SCMRevision rev = getSCMRevision(run);
        scmHead = rev.getHead().getName();
        scmHash = rev.toString();
        if (prop != null && !isBranchExcluded(scmHead, prop.getBranchExcludes())) {
            excludeSyntheticSteps = prop.isExcludeSyntheticSteps();
            excludeMetaSteps = prop.isExcludeMetaSteps();
            HelidonPublisherServer server = prop.getServer();
            if (server != null) {
                publisherServerUrl = server.getServerUrl();
                publisherClientThreads = server.getNthread();
            } else {
                publisherServerUrl = null;
                publisherClientThreads = 5;
            }
            id = createId(jobName, scmHead, scmHash, run.getNumber(), run.getTimeInMillis());
            // TODO repository URL
        } else {
            id = null;
            publisherServerUrl = null;
            publisherClientThreads = 0;
            excludeSyntheticSteps = true;
            excludeMetaSteps = true;
        }
    }

    PipelineRunInfo(Run<?, ?> run) {
        HelidonPublisherProjectProperty prop = run.getParent().getProperty(HelidonPublisherProjectProperty.class);
        jobName = run.getParent().getName();
        SCMRevision rev = getSCMRevision(run);
        scmHead = rev.getHead().getName();
        scmHash = rev.toString();
        if (prop != null && !isBranchExcluded(scmHead, prop.getBranchExcludes())) {
            excludeSyntheticSteps = prop.isExcludeSyntheticSteps();
            excludeMetaSteps = prop.isExcludeMetaSteps();
            HelidonPublisherServer server = prop.getServer();
            if (server != null) {
                publisherServerUrl = server.getServerUrl();
                publisherClientThreads = server.getNthread();
            } else {
                publisherServerUrl = null;
                publisherClientThreads = 5;
            }
            id = createId(jobName, scmHead, scmHash, run.getNumber(), run.getTimeInMillis());
        } else {
            id = null;
            publisherServerUrl = null;
            publisherClientThreads = 0;
            excludeSyntheticSteps = true;
            excludeMetaSteps = true;
        }
    }

    /**
     * Test if this run should be processed.
     * @return {@code true} if enabled, {@code false} otherwise
     */
    boolean isEnabled() {
        return id != null;
    }

    @Override
    public String toString() {
        return PipelineRunInfo.class.getSimpleName() + "{"
                + " id=" + id == null ? "null" : id
                + ", jobName=" + jobName
                + ", scmHead=" + scmHead
                + ", scmHash=" + scmHash
                + ", publisherServerUrl=" + publisherServerUrl == null ? "null" : publisherServerUrl
                + ", publisherClientThreads=" + publisherClientThreads == null ? "null" : publisherClientThreads
                + ", excludeSyntheticSteps=" + excludeSyntheticSteps
                + ", excludeMetaSteps=" + excludeMetaSteps
                + " }";
    }

    /**
     * Create a unique ID.
     * @param jobName job name
     * @param scmHead SCM head
     * @param scmHash SCM hash
     * @param buildNumber build number
     * @param startTime start timestamp
     * @return String
     */
    private static String createId(String jobName, String scmHead, String scmHash, int buildNumber, long startTime) {
        String runDesc = jobName + "/" + scmHead + "/" + buildNumber + "/" + startTime + "/" + scmHash;
        return md5sum(runDesc.getBytes());
    }

    /**
     * Make a MD5 sum string out of the input data.
     *
     * @param data data to checksum
     * @return String
     */
    private static String md5sum(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);
            byte[] digest = md.digest();
            StringBuilder r = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                r.append(HEX_CODE[(b >> 4) & 0xF]);
                r.append(HEX_CODE[(b & 0xF)]);
            }
            return r.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Test if the given branch name is part of the given excludes list.
     *
     * @param branch branch to test,
     * @param excludes space separated list of branch names
     * @return {@code true} if excluded, {@code false} otherwise
     */
    private static boolean isBranchExcluded(String branch, String excludes) {
        if (excludes != null) {
            for (String exclude : excludes.split(" ")) {
                if (branch.equals(exclude)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the SCM revision for this run.
     * @param run the run
     * @return SCMRevision
     */
    private static SCMRevision getSCMRevision(Actionable run) {
        SCMRevisionAction revAction = run.getAction(SCMRevisionAction.class);
        if (revAction != null) {
            return revAction.getRevision();
        }
        throw new IllegalStateException("Unable to get scm revision");
    }
}
