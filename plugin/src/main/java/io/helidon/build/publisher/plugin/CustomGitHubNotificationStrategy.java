package io.helidon.build.publisher.plugin;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static final Logger LOGGER = Logger.getLogger(CustomGitHubNotificationStrategy.class.getName());
    private static final String DEFAULT_MESSAGE_PREFIX = "continuous-integration/jenkins/";

    @Override
    public List<GitHubNotificationRequest> notifications(GitHubNotificationContext ctx, TaskListener listener) {
        HelidonPublisherServer pubServer = HelidonPublisherServer.get(ctx.getBuild());
        String serverUrl = pubServer.getServerUrl();
        Run<?, ?> run = ctx.getBuild();
        if (serverUrl != null) {
            String pipelineId = null;
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
                String statusUrl = serverUrl + "/" + pipelineId;
                String context = ctx.getDefaultContext(listener).replace(DEFAULT_MESSAGE_PREFIX, "ci/");
                String message = ctx.getDefaultMessage(listener);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Creating commit status, url={0}, context={1}, message={2}", new Object[] {
                        statusUrl,
                        context,
                        message
                    });
                }
                return Collections.singletonList(
                        GitHubNotificationRequest.build(context, statusUrl, message, ctx.getDefaultState(listener),
                                ctx.getDefaultIgnoreError(listener)));
            }
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Skipping commit status for job: {0}", run);
        }
        return Collections.emptyList();
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
