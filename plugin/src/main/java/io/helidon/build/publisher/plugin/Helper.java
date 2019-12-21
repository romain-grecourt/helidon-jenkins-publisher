package io.helidon.build.publisher.plugin;

import hudson.model.ItemGroup;
import hudson.model.Queue;
import hudson.model.Result;
import io.helidon.build.publisher.model.Status;
import java.io.IOException;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

/**
 * Utility class.
 */
final class Helper {

    private Helper() {}

    /**
     * Get the workflow run or the given execution.
     * @param owner
     * @return WorkflowRun
     */
    static WorkflowRun getRun(FlowExecutionOwner owner) {
        try {
            Queue.Executable exec = owner.getExecutable();
            if (exec instanceof WorkflowRun) {
                return (WorkflowRun) exec;
            }
            throw new IllegalStateException("Unable to get flow run from execution owner");
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Get the top level multi branch project for the given job.
     * @param job workflow job
     * @return WorkflowMultiBranchProject
     */
    static WorkflowMultiBranchProject getProject(WorkflowJob job) {
        ItemGroup ig = job.getParent();
        if (ig instanceof WorkflowMultiBranchProject) {
            return (WorkflowMultiBranchProject) ig;
        }
        throw new IllegalStateException("Unable to get multibranch project");
    }

    /**
     * Convert a {@link Result} instance to a {@link FlowStatus.Result}.
     * @param result result to convert
     * @return FlowStatus.Result
     */
    static Status.Result convertResult(Result result) {
        if (result == null) {
            return Status.Result.UNKNOWN;
        }
        switch (result.ordinal) {
            case 4:
                return Status.Result.ABORTED;
            case 3:
                return Status.Result.NOT_BUILT;
            case 2:
                return Status.Result.FAILURE;
            case 1:
                return Status.Result.UNSTABLE;
            case 0:
                return Status.Result.SUCCESS;
            default:
                return Status.Result.NOT_BUILT;
        }
    }
}
