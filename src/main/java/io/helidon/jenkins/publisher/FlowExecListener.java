package io.helidon.jenkins.publisher;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;

/**
 * Listen to flow run overall states.
 */
@Extension
public final class FlowExecListener extends FlowExecutionListener {

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
