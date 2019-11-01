package io.helidon.jenkins.publisher;

import io.helidon.jenkins.publisher.config.HelidonPublisherFolderProperty;
import java.io.IOException;
import java.io.OutputStream;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.log.TaskListenerDecorator;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.kohsuke.accmod.restrictions.suppressions.SuppressRestrictedWarnings;

/**
 * Decorator that matches node head with console output and selectively decorates it.
 */
@SuppressRestrictedWarnings({TaskListenerDecorator.class})
final class FlowDecorator extends TaskListenerDecorator implements GraphListener.Synchronous, FlowGraph.StatusListener {

    private final boolean enabled;
    private final FlowGraph graph;
    private final String scmHead;
    private final String scmHash;
    private final String publisherUrl;

    FlowDecorator(FlowExecution execution) {
        WorkflowRun run = FlowHelper.getRun(execution.getOwner());
        WorkflowMultiBranchProject project = FlowHelper.getProject(run.getParent());
        HelidonPublisherFolderProperty prop = project.getProperties().get(HelidonPublisherFolderProperty.class);
        SCMRevision rev = getSCMRevision(run);
        scmHead = rev.getHead().getName();
        scmHash = rev.toString();
        if (prop != null && !isBranchExcluded(scmHead, prop.getBranchExcludes())) {
            graph = new FlowGraph(FlowStepSignatures.getOrCreate(execution));
            publisherUrl = prop.getServerUrl();
            enabled = true;
        } else {
            enabled = false;
            publisherUrl = null;
            graph = null;
        }
    }

    /**
     * Get the flow graph.
     * @return FlowGraph
     */
    FlowGraph graph() {
        return graph;
    }

    /**
     * Test if this decorator is enabled.
     * @return {@code true} if enabled, {@code false} otherwise
     */
    boolean isEnabled() {
        return enabled;
    }

    @Override
    public OutputStream decorate(OutputStream out) throws IOException, InterruptedException {
        if (enabled) {
            FlowStep step = graph.poll();
            if (step != null && step.declared() && !step.meta()) {
                return new FlowStepOutputStream(out, step);
            }
        }
        return out;
    }

    @Override
    public void onNewHead(FlowNode node) {
        if (enabled) {
            graph.offer(node, /* status listener */ this);
        }
    }

    @Override
    public void onStepStatus(FlowStep step, FlowStatus status) {
        System.out.println("onStepStatus: " + step.id() + ", status:" + status.result);
    }

    @Override
    public void onStageStatus(FlowStage stage, FlowStatus status) {
        System.out.println("onStageStatus: " + stage.id() + ", status:" + status.result);
    }

    /**
     * Get the SCM revision for this run.
     * @param run the flow run
     * @return SCMRevision
     */
    private static SCMRevision getSCMRevision(WorkflowRun run) {
        SCMRevisionAction revAction = run.getAction(SCMRevisionAction.class);
        if (revAction != null) {
            return revAction.getRevision();
        }
        throw new IllegalStateException("Unable to get scm revision");
    }

    /**
     * Test if the given branch name is part of the given excludes list.
     *
     * @param branch branch to test,
     * @param excludes space separated list of branch names
     * @return {@code true} if excluded, {@code false} otherwise
     */
    private static boolean isBranchExcluded(String branch, String excludes) {
        if (excludes != null) {
            for (String exclude : excludes.split(" ")) {
                if (branch.equals(exclude)) {
                    return true;
                }
            }
        }
        return false;
    }
}
