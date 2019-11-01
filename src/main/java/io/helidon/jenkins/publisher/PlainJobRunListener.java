package io.helidon.jenkins.publisher;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import io.helidon.jenkins.publisher.config.HelidonPublisherProjectProperty;
import io.helidon.jenkins.publisher.config.HelidonPublisherServer;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

@Extension
public final class PlainJobRunListener extends RunListener<Run<?,?>> {

    @Override
    public void onStarted(Run run, TaskListener listener) {
        if (isEnabled(run)) {
            System.out.println("onStarted - " + run.getId());
        }
    }

    @Override
    public void onCompleted(Run run, @Nonnull TaskListener listener) {
        if (isEnabled(run)) {
            Result result = run.getResult();
            System.out.println("onCompleted - " + run.getId() + " - " + result != null ? result.toString() : "?");
        }
    }

    /**
     * Test if the {@link run} instance is not flow related and has a {@link HelidonPublisherProjectProperty} property.
     * @param run
     * @return {@code true} if enabled for the given {@link run}, {@code false} otherwise
     */
    private static boolean isEnabled(Run run) {
        if (run instanceof WorkflowRun || run == null) {
            return false;
        }
        return HelidonPublisherServer.get(run) != null;
    }

}