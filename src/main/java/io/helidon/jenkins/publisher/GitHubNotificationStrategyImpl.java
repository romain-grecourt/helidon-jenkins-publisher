package io.helidon.jenkins.publisher;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.helidon.jenkins.publisher.config.HelidonPublisherFolderProperty;
import org.jenkinsci.plugins.github_branch_source.AbstractGitHubNotificationStrategy;
import org.jenkinsci.plugins.github_branch_source.GitHubNotificationContext;
import org.jenkinsci.plugins.github_branch_source.GitHubNotificationRequest;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

@Extension
public class GitHubNotificationStrategyImpl extends AbstractGitHubNotificationStrategy{

    @Override
    public List<GitHubNotificationRequest> notifications(GitHubNotificationContext notificationContext, TaskListener listener) {
        return Collections.singletonList(GitHubNotificationRequest.build(
                notificationContext.getDefaultContext(listener),
                getBackRefUrl(notificationContext),
                notificationContext.getDefaultMessage(listener),
                notificationContext.getDefaultState(listener),
                notificationContext.getDefaultIgnoreError(listener)));
    }

    private static String getBackRefUrl(GitHubNotificationContext notificationContext) {
        Run<?, ?> run = notificationContext.getBuild();
        if (run != null) {
            if (run instanceof WorkflowRun) {
                ItemGroup itemGroup = ((WorkflowRun) run).getParent().getParent();
                if (itemGroup instanceof AbstractFolder<?>) {
                    AbstractFolder<?> folder = (AbstractFolder<?>) itemGroup;
                    HelidonPublisherFolderProperty prop = folder.getProperties().get(HelidonPublisherFolderProperty.class);
                    if (prop != null) {
                        String serverUrl = prop.getServerUrl();
                        if (serverUrl != null) {
                            return serverUrl;
                        }
                    }
                }
            }
        }
        return "#";
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
