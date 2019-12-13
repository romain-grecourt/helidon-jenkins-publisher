package io.helidon.build.publisher.plugin;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import io.helidon.build.publisher.model.PipelineInfo;
import io.helidon.build.publisher.plugin.config.HelidonPublisherFolderProperty;
import io.helidon.build.publisher.plugin.config.HelidonPublisherProjectProperty;
import io.helidon.build.publisher.plugin.config.HelidonPublisherServer;

import hudson.model.Actionable;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.scm.SCM;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import jenkins.triggers.SCMTriggerItem;
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
    final String title;
    final String scmHead;
    final String scmHash;
    final String repositoryUrl;
    final String publisherServerUrl;
    final int publisherClientThreads;
    final long startTime;

    PipelineRunInfo() {
        id = null;
        excludeSyntheticSteps = false;
        excludeMetaSteps = false;
        publisherClientThreads = 0;
        title = null;
        repositoryUrl = null;
        scmHead = null;
        scmHash = null;
        publisherServerUrl = null;
        startTime = 0;
    }

    PipelineRunInfo(FlowExecution execution) {
        WorkflowRun run = Helper.getRun(execution.getOwner());
        startTime = run.getStartTimeInMillis();
        WorkflowMultiBranchProject project = Helper.getProject(run.getParent());
        title = project.getName();
        HelidonPublisherFolderProperty prop = project.getProperties().get(HelidonPublisherFolderProperty.class);
        SCMRevisionAction revAction = getSCMRevisionAction(run);
        SCMRevision rev = revAction.getRevision();
        SCMSource scmSource = project.getSCMSource(revAction.getSourceId());
        if (scmSource instanceof AbstractGitSCMSource) {
            String remote = ((AbstractGitSCMSource)scmSource).getRemote();
            repositoryUrl = remote;
        } else {
            repositoryUrl = null;
        }
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
            id = createId(title, repositoryUrl, scmHead, scmHash, run.getNumber(), run.getTimeInMillis());
        } else {
            id = null;
            publisherServerUrl = null;
            publisherClientThreads = 0;
            excludeSyntheticSteps = true;
            excludeMetaSteps = true;
        }
    }

    PipelineRunInfo(Run<?, ?> run) {
        Job<?, ?> job = run.getParent();
        HelidonPublisherProjectProperty prop = job.getProperty(HelidonPublisherProjectProperty.class);
        title = run.getParent().getName();
        SCMRevisionAction revAction = getSCMRevisionAction(run);
        String remote = null;
        if (job instanceof SCMTriggerItem) {
            SCMTriggerItem trigger = (SCMTriggerItem) job;
            Iterator<? extends SCM> it = trigger.getSCMs().iterator();
            while(it.hasNext() && remote == null) {
                SCM scm = it.next();
                if (scm instanceof GitSCM) {
                    for (UserRemoteConfig urc : ((GitSCM) scm).getUserRemoteConfigs()) {
                        if (revAction.getSourceId().equals(urc.getName())) {
                            remote = urc.getUrl();
                            break;
                        }
                    }
                }
            }
        }
        startTime = run.getStartTimeInMillis();
        repositoryUrl = remote;
        SCMRevision rev = revAction.getRevision();
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
            id = createId(title, repositoryUrl, scmHead, scmHash, run.getNumber(), run.getTimeInMillis());
        } else {
            id = null;
            publisherServerUrl = null;
            publisherClientThreads = 0;
            excludeSyntheticSteps = true;
            excludeMetaSteps = true;
        }
    }

    PipelineInfo toPipelineInfo() {
        return new PipelineInfo(id, title, repositoryUrl, scmHead, scmHash);
    }

    @Override
    public String toString() {
        return PipelineRunInfo.class.getSimpleName() + "{"
                + " id=" + id == null ? "null" : id
                + ", title=" + title
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
     * @param title job name
     * @param scmHead SCM head
     * @param scmHash SCM hash
     * @param buildNumber build number
     * @param startTime start timestamp
     * @return String
     */
    private static String createId(String title, String repotistoryUrl, String scmHead, String scmHash, int buildNumber,
            long startTime) {

        String runDesc = title + "/" + repotistoryUrl + "/" + scmHead + "/" + buildNumber + "/" + startTime + "/" + scmHash;
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

    private static SCMRevisionAction getSCMRevisionAction(Actionable run) {
        SCMRevisionAction revAction = run.getAction(SCMRevisionAction.class);
        if (revAction != null) {
            return revAction;
        }
        throw new IllegalStateException("Unable to get scm revision");
    }
}
