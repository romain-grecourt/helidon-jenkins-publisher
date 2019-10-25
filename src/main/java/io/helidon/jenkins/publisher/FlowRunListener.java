package io.helidon.jenkins.publisher;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;
import org.jenkinsci.plugins.workflow.log.LogStorage;
import org.kohsuke.accmod.restrictions.suppressions.SuppressRestrictedWarnings;

/**
 * Listen to flow run overall states.
 */
@SuppressRestrictedWarnings(LogStorage.class)
@Extension
public final class FlowRunListener extends FlowExecutionListener {

    @Override
    public void onCompleted(FlowExecution execution) {
        FlowDecoratorFactory.clear(execution);
    }

    @Override
    public void onResumed(FlowExecution execution) {
    }

    @Override
    public void onRunning(FlowExecution execution) {
    }
}
