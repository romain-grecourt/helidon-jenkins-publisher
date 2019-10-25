package io.helidon.jenkins.publisher;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

@Extension
public final class PlainRunListener extends RunListener<Run<?,?>> {

    @Override
    public void onStarted(Run run, TaskListener listener) {
    }

    @Override
    public void onCompleted(Run run, @Nonnull TaskListener listener) {
        if (run instanceof WorkflowRun || run == null) {
            return;
        }
        Result result = run.getResult();
        if (result != null) {
            System.out.println("onCompleted - " + run.getId()+ " - " + result.toString());
        }
    }

}