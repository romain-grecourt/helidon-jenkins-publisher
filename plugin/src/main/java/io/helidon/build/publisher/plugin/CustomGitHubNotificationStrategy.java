package io.helidon.build.publisher.plugin;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import hudson.Extension;
import hudson.model.TaskListener;
import io.helidon.build.publisher.plugin.config.HelidonPublisherServer;
import org.jenkinsci.plugins.github_branch_source.AbstractGitHubNotificationStrategy;
import org.jenkinsci.plugins.github_branch_source.GitHubNotificationContext;
import org.jenkinsci.plugins.github_branch_source.GitHubNotificationRequest;

/**
 * A GitHub notification strategy that customizes the commit status URL to point at the (external) publisher server.
 */
@Extension
public class CustomGitHubNotificationStrategy extends AbstractGitHubNotificationStrategy {

    @Override
    public List<GitHubNotificationRequest> notifications(GitHubNotificationContext notificationContext, TaskListener listener) {
        HelidonPublisherServer pubServer = HelidonPublisherServer.get(notificationContext.getBuild());
        return Collections.singletonList(GitHubNotificationRequest.build(
                notificationContext.getDefaultContext(listener),
                pubServer == null ? "#" : pubServer.getServerUrl(),
                notificationContext.getDefaultMessage(listener),
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
