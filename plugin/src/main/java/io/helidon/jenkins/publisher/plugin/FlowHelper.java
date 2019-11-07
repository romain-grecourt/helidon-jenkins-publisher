package io.helidon.jenkins.publisher.plugin;

import hudson.model.ItemGroup;
import hudson.model.Queue;
import java.io.IOException;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

/**
 * Flow utility class.
 */
final class FlowHelper {

    private FlowHelper() {}

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
}
