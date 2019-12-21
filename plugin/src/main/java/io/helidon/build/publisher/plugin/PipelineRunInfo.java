package io.helidon.build.publisher.plugin;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import io.helidon.build.publisher.model.PipelineInfo;
import io.helidon.build.publisher.model.Status;
import io.helidon.build.publisher.model.Timings;
import io.helidon.build.publisher.plugin.config.HelidonPublisherFolderProperty;
import io.helidon.build.publisher.plugin.config.HelidonPublisherProjectProperty;
import io.helidon.build.publisher.plugin.config.HelidonPublisherServer;

import hudson.model.Actionable;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.scm.SCM;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.mixin.ChangeRequestSCMHead;
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
    final SCMInfo scmInfo;
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
        scmInfo = null;
        publisherServerUrl = null;
        startTime = 0;
    }

    private static final class SCMInfo {

        final String headRef;
        final String commit;
        final String mergeCommit;

        SCMInfo(SCMRevision rev) {
            SCMHead scmHead = rev.getHead();
            if (scmHead instanceof ChangeRequestSCMHead) {
                headRef = "pull/" + ((ChangeRequestSCMHead) scmHead).getId();
                String revCommit = rev.toString();
                int idx = revCommit.indexOf("+");
                if (idx > 0) {
                    commit = revCommit.substring(0, idx);
                    int idx2 = revCommit.indexOf(" ");
                    if (idx2 > 0) {
                        mergeCommit = revCommit.substring(idx + 1, idx2);
                    } else {
                        mergeCommit = revCommit.substring(idx + 1);
                    }
                } else {
                    commit = revCommit;
                    mergeCommit = null;
                }
            } else {
                headRef = rev.getHead().getName();
                commit = rev.toString();
                mergeCommit = null;
            }
        }

        @Override
        public String toString() {
            return SCMInfo.class.getSimpleName() + " {"
                    + " headRef=" + headRef
                    + ", commit=" + commit
                    + ", mergeCommit=" + mergeCommit
                    + " }";
        }
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
        scmInfo = new SCMInfo(rev);
        if (prop != null && !isBranchExcluded(scmInfo.headRef, prop.getBranchExcludes())) {
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
            id = createId(title, repositoryUrl, scmInfo.headRef, scmInfo.commit, run.getNumber(), run.getTimeInMillis());
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
                        String sourceId = revAction.getSourceId();
                        if (sourceId != null && sourceId.equals(urc.getName())) {
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
        scmInfo = new SCMInfo(rev);
        if (prop != null && !isBranchExcluded(scmInfo.headRef, prop.getBranchExcludes())) {
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
            id = createId(title, repositoryUrl, scmInfo.headRef, scmInfo.commit, run.getNumber(), run.getTimeInMillis());
        } else {
            id = null;
            publisherServerUrl = null;
            publisherClientThreads = 0;
            excludeSyntheticSteps = true;
            excludeMetaSteps = true;
        }
    }

    /**
     * Create a {@link PipelineInfo} from this run info.
     * @param status status object
     * @param timings timing object
     * @return PipelineInfo
     */
    PipelineInfo toPipelineInfo(Status status, Timings timings) {
        return PipelineInfo.builder()
                .id(id)
                .title(title)
                .repositoryUrl(repositoryUrl)
                .headRef(scmInfo.headRef)
                .commit(scmInfo.commit)
                .mergeCommit(scmInfo.mergeCommit)
                .status(status)
                .timings(timings)
                .build();
    }

    @Override
    public String toString() {
        return PipelineRunInfo.class.getSimpleName() + "{"
                + " id=" + id
                + ", title=" + title
                + ", repositoryUrl=" + repositoryUrl
                + ", scmIfno=" + scmInfo
                + ", publisherServerUrl=" + publisherServerUrl
                + ", publisherClientThreads=" + publisherClientThreads
                + ", excludeSyntheticSteps=" + excludeSyntheticSteps
                + ", excludeMetaSteps=" + excludeMetaSteps
                + " }";
    }

    /**
     * Create a unique ID.
     * @param title job name
     * @param headRef GIT head ref
     * @param commit GIT commit
     * @param buildNumber build number
     * @param startTime start timestamp
     * @return String
     */
    private static String createId(String title, String repotistoryUrl, String headRef, String commit, int buildNumber,
            long startTime) {

        String runDesc = title + "/" + repotistoryUrl + "/" + headRef + "/" + buildNumber + "/" + startTime + "/" + commit;
        try {
            return md5sum(runDesc.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
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
                if (Matcher.match(branch, exclude)) {
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
