package io.helidon.jenkins.publisher;

import hudson.console.ConsoleNote;
import hudson.console.LineTransformationOutputStream;
import hudson.model.ItemGroup;
import hudson.model.Queue;
import io.helidon.jenkins.publisher.config.HelidonPublisherFolderProperty;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.log.TaskListenerDecorator;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.kohsuke.accmod.restrictions.suppressions.SuppressRestrictedWarnings;

/**
 * Decorator that matches node head with console output and selectively decorates it.
 */
@SuppressRestrictedWarnings({TaskListenerDecorator.class})
final class FlowDecorator extends TaskListenerDecorator implements GraphListener.Synchronous {

    private final boolean enabled;
    private final LinkedList<FlowStep> headNodes = new LinkedList<>();
    private final Map<String, FlowStep> allSteps = new ConcurrentHashMap<>();
    private final FlowStepSignatures signatures;
    private final String scmHead;
    private final String scmHash;
    private final String publisherUrl;

    FlowDecorator(FlowExecution execution) {
        WorkflowRun run = getRun(execution.getOwner());
        WorkflowMultiBranchProject project = getProject(run.getParent());
        HelidonPublisherFolderProperty prop = project.getProperties().get(HelidonPublisherFolderProperty.class);
        SCMRevisionAction revAction = run.getAction(SCMRevisionAction.class);
        if (revAction != null) {
            SCMRevision rev = revAction.getRevision();
            this.scmHead = rev.getHead().getName();
            this.scmHash = rev.toString();
        } else {
            throw new IllegalStateException("Unable to get scm revision");
        }
        if (prop != null) {
            enabled = !isBranchExcluded(scmHead, prop.getBranchExcludes());
            publisherUrl = prop.getServerUrl();
        } else {
            enabled = false;
            publisherUrl = null;
        }
        this.signatures = FlowStepSignatures.getOrCreate(execution);
    }

    @Override
    public OutputStream decorate(OutputStream out) throws IOException, InterruptedException {
        if (enabled && !headNodes.isEmpty()) {
            FlowStep step = headNodes.pop();
            if (step.isDeclared() && !step.isMeta()) {
                allSteps.put(step.getId(), step);
                return new StepOutputStream(out, step);
            }
        }
        return out;
    }

    private void checkStatus(){
        for (Entry<String, FlowStep> entry : allSteps.entrySet()) {
            FlowStep step = entry.getValue();
            String id = entry.getKey();
            FlowStatus status = new FlowStatus(step.getNode());
            if (status.state == FlowStatus.FlowState.FINISHED) {
                System.out.println("FlowStep["+ id + "] FINISHED: " + status.result.name());
            }
            allSteps.remove(id);
        }
    }

    @Override
    public void onNewHead(FlowNode node) {
        if ((node instanceof StepAtomNode)) {
            this.headNodes.addLast(new FlowStep((StepAtomNode) node, signatures));
        }
        checkStatus();
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

    /**
     * Get the workflow run or the given execution.
     * @param owner
     * @return WorkflowRun
     */
    private static WorkflowRun getRun(FlowExecutionOwner owner) {
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
    private static WorkflowMultiBranchProject getProject(WorkflowJob job) {
        ItemGroup ig = job.getParent();
        if (ig instanceof WorkflowMultiBranchProject) {
            return (WorkflowMultiBranchProject) ig;
        }
        throw new IllegalStateException("Unable to get multibranch project");
    }

    /**
     * OutputStream wrapper to intercept step output.
     */
    private static final class StepOutputStream extends LineTransformationOutputStream {

        private static final AtomicInteger IDS = new AtomicInteger();
        private final OutputStream out;
        private final FlowStep step;
        private final int id;

        /**
         * Create a new instance.
         *
         * @param out the stream to wrap
         * @param step the associated step
         */
        StepOutputStream(OutputStream out, FlowStep step) {
            super();
            this.out = out;
            this.step = step;
            this.id = IDS.incrementAndGet();
        }

        @Override
        public void flush() throws IOException {
            if (out != null) {
                out.flush();
            }
        }

        @Override
        public void close() throws IOException {
            if (out != null) {
                out.close();
            }
        }

        @Override
        protected void eol(byte[] bytes, int len) throws IOException {
            if (ConsoleNote.findPreamble(bytes, 0, len) == -1) {
                System.out.print("[" + this.toString() + "] ");
                System.out.write(bytes, 0, len);
            }
            if (out != null) {
                out.write(bytes, 0, len);
            }
        }

        @Override
        public String toString() {
            return StepOutputStream.class.getSimpleName() + "{"
                    + "id=" + id
                    + ", step=" + step.getId()
                    + "}";
        }
    }
}
