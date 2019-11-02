package io.helidon.jenkins.publisher;

import hudson.model.Result;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.NotExecutedNodeAction;
import org.jenkinsci.plugins.workflow.actions.QueueItemAction;
import org.jenkinsci.plugins.workflow.actions.WarningAction;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;

/**
 * Flow status.
 */
final class FlowStatus {

    /**
     * Flow result.
     */
    enum FlowResult {
        ABORTED,
        FAILURE,
        UNKNOWN,
        NOT_BUILT,
        UNSTABLE,
        SUCCESS;

        static FlowResult fromResult(Result res) {
            if (res == null) {
                return NOT_BUILT;
            }
            switch (res.ordinal) {
                case 4:
                    return ABORTED;
                case 3:
                    return NOT_BUILT;
                case 2:
                    return FAILURE;
                case 1:
                    return UNSTABLE;
                case 0:
                    return SUCCESS;
                default:
                    return NOT_BUILT;
            }
        }
    }

    /**
     * Flow state.
     */
    enum FlowState {
        QUEUED,
        RUNNING,
        FINISHED
    }

    final FlowResult result;
    final FlowState state;

    FlowStatus(FlowNode node) {
        if (node == null) {
            this.result = FlowResult.UNKNOWN;
            this.state = FlowState.QUEUED;
        } else {
            Result res = null;
            ErrorAction errorAction = node.getError();
            WarningAction warningAction = node.getPersistentAction(WarningAction.class);
            if (errorAction != null) {
                if (errorAction.getError() instanceof FlowInterruptedException) {
                    res = ((FlowInterruptedException) errorAction.getError()).getResult();
                }
                if (res == null || res != Result.ABORTED) {
                    this.result = FlowResult.FAILURE;
                } else {
                    this.result = FlowResult.ABORTED;
                }
                this.state = node.isActive() ? FlowState.RUNNING : FlowState.FINISHED;
            } else if (warningAction != null) {
                this.result = FlowResult.fromResult(warningAction.getResult());
                this.state = node.isActive() ? FlowState.RUNNING : FlowState.FINISHED;
            } else if (QueueItemAction.getNodeState(node) == QueueItemAction.QueueState.QUEUED) {
                this.result = FlowResult.UNKNOWN;
                this.state = FlowState.QUEUED;
            } else if (QueueItemAction.getNodeState(node) == QueueItemAction.QueueState.CANCELLED) {
                this.result = FlowResult.ABORTED;
                this.state = FlowState.FINISHED;
            } else if (node.isActive()) {
                this.result = FlowResult.UNKNOWN;
                this.state = FlowState.RUNNING;
            } else if (NotExecutedNodeAction.isExecuted(node)) {
                this.result = FlowResult.SUCCESS;
                this.state = FlowState.FINISHED;
            } else {
                this.result = FlowResult.NOT_BUILT;
                this.state = FlowState.QUEUED;
            }
        }
    }

    @Override
    public String toString() {
        return FlowStatus.class.getSimpleName() + "{"
                + " state=" + state
                + ", result=" + result
                + "}";
    }
}
