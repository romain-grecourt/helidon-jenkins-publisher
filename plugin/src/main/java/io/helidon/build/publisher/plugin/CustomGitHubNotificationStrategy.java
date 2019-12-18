package io.helidon.build.publisher.plugin;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.helidon.build.publisher.plugin.config.HelidonPublisherServer;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github_branch_source.AbstractGitHubNotificationStrategy;
import org.jenkinsci.plugins.github_branch_source.GitHubNotificationContext;
import org.jenkinsci.plugins.github_branch_source.GitHubNotificationRequest;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

/**
 * A GitHub notification strategy that customizes the commit status URL to point at the (external) publisher server.
 */
@Extension
public class CustomGitHubNotificationStrategy extends AbstractGitHubNotificationStrategy {

    private static final String DEFAULT_MESSAGE_PREFIX = "continuous-integration/jenkins/";

    @Override
    public List<GitHubNotificationRequest> notifications(GitHubNotificationContext notificationContext, TaskListener listener) {
        HelidonPublisherServer pubServer = HelidonPublisherServer.get(notificationContext.getBuild());
        String statusUrl = pubServer.getServerUrl();
        if (statusUrl == null) {
            statusUrl = "#";
        } else {
            String pipelineId = null;
            Run<?, ?> run = notificationContext.getBuild();
            if (run instanceof WorkflowRun) {
                PipelinePublisher publisher = PipelinePublisher.get((WorkflowRun) run);
                if (publisher != null) {
                    pipelineId = publisher.pipelineId();
                }
            } else {
                JobPublisher publisher = JobPublisher.get(run);
                if (publisher != null) {
                    pipelineId = publisher.pipelineId();
                }
            }
            if (pipelineId != null) {
                statusUrl += "/" + pipelineId;
            } else {
                statusUrl = "#";
            }
        }
        return Collections.singletonList(GitHubNotificationRequest.build(
                notificationContext.getDefaultContext(listener),
                statusUrl,
                notificationContext.getDefaultMessage(listener).replace(DEFAULT_MESSAGE_PREFIX, ""),
                notificationContext.getDefaultState(listener),
                notificationContext.getDefaultIgnoreError(listener)));
    }

    @Override
    public boolean equals(Object o) {
        return Objects.equals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this);
    }
}
