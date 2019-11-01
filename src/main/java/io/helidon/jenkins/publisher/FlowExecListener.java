package io.helidon.jenkins.publisher;

import hudson.Extension;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

/**
 * Listen to flow run overall states.
 */
@Extension
public final class FlowExecListener extends FlowExecutionListener {

    @Override
    public void onCompleted(FlowExecution execution) {
        WorkflowRun run = FlowHelper.getRun(execution.getOwner());
        Result result = run.getResult();
        System.out.println("Flow run completed: " + run.getFullDisplayName() + ", status: " + result);
        FlowDecorator decorator = FlowDecoratorFactory.clear(execution);
        if (decorator != null && decorator.isEnabled()) {
            System.out.println(decorator.graph().root().prettyPrint("", true, true));
        }
    }

    @Override
    public void onResumed(FlowExecution execution) {
    }

    @Override
    public void onRunning(FlowExecution execution) {
        WorkflowRun run = FlowHelper.getRun(execution.getOwner());
        System.out.println("Flow run onRunning: " + run.getFullDisplayName());
    }
}
