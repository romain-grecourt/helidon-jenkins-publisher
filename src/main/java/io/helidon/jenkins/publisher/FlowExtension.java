package io.helidon.jenkins.publisher;

import hudson.Extension;
import hudson.model.Result;
import io.helidon.jenkins.publisher.config.HelidonPublisherFolderProperty;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.log.TaskListenerDecorator;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.kohsuke.accmod.restrictions.suppressions.SuppressRestrictedWarnings;

/**
 * Implements both a {@link TaskListenerDecorator} and {@link GraphListener.Synchronous} in order to intercept all relevant
 * events flow events.
 */
@SuppressRestrictedWarnings({TaskListenerDecorator.class})
final class FlowExtension extends TaskListenerDecorator implements GraphListener.Synchronous, FlowGraph.EventListener {

    private final boolean enabled;
    private final FlowGraph graph;
    private final String scmHead;
    private final String scmHash;
    private final String publisherUrl;
    private boolean declaredOnly;
    private boolean skipMeta;

    FlowExtension(FlowExecution execution) {
        WorkflowRun run = FlowHelper.getRun(execution.getOwner());
        WorkflowMultiBranchProject project = FlowHelper.getProject(run.getParent());
        HelidonPublisherFolderProperty prop = project.getProperties().get(HelidonPublisherFolderProperty.class);
        SCMRevision rev = getSCMRevision(run);
        scmHead = rev.getHead().getName();
        scmHash = rev.toString();
        if (prop != null && !isBranchExcluded(scmHead, prop.getBranchExcludes())) {
            graph = new FlowGraph(FlowStepSignatures.getOrCreate(execution));
            publisherUrl = prop.getServerUrl();
            declaredOnly = true; // TODO make a folder/job config option for this
            skipMeta = true; // TODO make a folder/job config option for this
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
            if (step != null && declaredOnly && step.declared() && skipMeta && !step.meta()) {
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
    public void onFlowStart(FlowStage.Sequence root) {
        System.out.println("onFlowStart");
    }

    @Override
    public void onFlowEnd(FlowStage.Sequence root) {
        System.out.println("onFlowEnd");
        System.out.println(graph.root().prettyPrint("", true, true));
    }

    @Override
    public void onStepEvent(FlowStep step, FlowGraph.Event event) {
        if (step.declared() && !step.meta()) {
            FlowStatus status = step.status();
            System.out.println("onStepStatus: " + step + ", event: " + event + " , state:" + status.state + ", result:" + status.result);
        }
    }

    @Override
    public void onStageEvent(FlowStage stage, FlowGraph.Event event) {
        if (stage.isSequence()) {
            FlowStatus status = stage.status();
            System.out.println("onStageStatus: " + stage + ", event: " + event + " , state:" + status.state + ", result:" + status.result);
        }
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

    private static final EmptyDecorator EMPTY_DECORATOR = new EmptyDecorator();
    private static final Map<FlowExecution, WeakReference<TaskListenerDecorator>> DECORATORS = new WeakHashMap<>();

    /**
     * TaskListenerDecorator factory.
     */
    @SuppressRestrictedWarnings({TaskListenerDecorator.Factory.class, TaskListenerDecorator.class})
    @Extension
    public static final class Factory implements TaskListenerDecorator.Factory {

        /**
         * Remove the decorator associated with the given execution.
         * @param exec the flow execution for which remove the cached decorator
         * @return the removed decorator
         */
        static FlowExtension clear(FlowExecution exec) {
            synchronized (DECORATORS) {
                WeakReference<TaskListenerDecorator> ref = DECORATORS.remove(exec);
                if (ref != null) {
                    TaskListenerDecorator decorator = ref.get();
                    if (decorator instanceof FlowExtension) {
                        return (FlowExtension) decorator;
                    }
                }
            }
            return null;
        }

        @Override
        public TaskListenerDecorator of(FlowExecutionOwner owner) {
            FlowExecution execution = owner.getOrNull();
            if (execution == null) {
                return EMPTY_DECORATOR;
            }
            synchronized (DECORATORS) {
                WeakReference<TaskListenerDecorator> decoratorRef = DECORATORS.get(execution);
                if (decoratorRef != null && decoratorRef.get() != null) {
                    return decoratorRef.get();
                }
            }
            FlowExtension decorator = new FlowExtension(execution);
            synchronized (DECORATORS) {
                WeakReference<TaskListenerDecorator> decoratorRef = DECORATORS.get(execution);
                if (decoratorRef != null && decoratorRef.get() != null) {
                    return decoratorRef.get();
                }
                TaskListenerDecorator dec;
                if (decorator.isEnabled()) {
                    dec = decorator;
                    execution.addListener(decorator);
                } else {
                    dec = EMPTY_DECORATOR;
                }
                DECORATORS.put(execution, new WeakReference<>(dec));
                return dec;
            }
        }
    }

    /**
     * No-op decorator.
     */
    @SuppressRestrictedWarnings(TaskListenerDecorator.class)
    private static final class EmptyDecorator extends TaskListenerDecorator {

        @Override
        public OutputStream decorate(OutputStream logger) throws IOException, InterruptedException {
            return logger;
        }
    }

    /**
     * {@link FlowExecutionListener} implementation used to clear the static cache of {@link TaskListenerDecorator}.
     */
    @Extension
    public static final class ExecutionListener extends FlowExecutionListener {

        @Override
        public void onCompleted(FlowExecution execution) {
            FlowExtension decorator = Factory.clear(execution);
            if (decorator == null) {
                WorkflowRun run = FlowHelper.getRun(execution.getOwner());
                Result result = run.getResult();
                System.out.println("TODO force push result: " + run.getFullDisplayName() + ", result: " + result);
            }
        }

        @Override
        public void onResumed(FlowExecution execution) {
        }

        @Override
        public void onRunning(FlowExecution execution) {
        }
    }
}
