package io.helidon.jenkins.publisher.plugin;

import hudson.Extension;
import hudson.model.Result;
import io.helidon.jenkins.publisher.plugin.config.HelidonPublisherFolderProperty;
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
final class FlowExtension extends TaskListenerDecorator implements GraphListener.Synchronous {

    private static final EmptyDecorator EMPTY_DECORATOR = new EmptyDecorator();
    private static final Map<FlowExecution, WeakReference<TaskListenerDecorator>> DECORATORS = new WeakHashMap<>();

    private final FlowGraph graph;
    private final HelidonPublisherClient client;

    FlowExtension(FlowExecution execution) {
        WorkflowRun run = FlowHelper.getRun(execution.getOwner());
        WorkflowMultiBranchProject project = FlowHelper.getProject(run.getParent());
        HelidonPublisherFolderProperty prop = project.getProperties().get(HelidonPublisherFolderProperty.class);
        SCMRevision rev = getSCMRevision(run);
        String scmHead = rev.getHead().getName();
        if (prop != null && !isBranchExcluded(scmHead, prop.getBranchExcludes())) {
            FlowStepSignatures signatures = FlowStepSignatures.getOrCreate(execution);
            graph = new FlowGraph(signatures, /* declaredOnly */ true, /* skipMeta */ true,
                    new FlowRun(project.getName(), scmHead, run.getNumber(), run.getTimeInMillis(), rev.toString()));
            client = HelidonPublisherClient.getOrCreate(prop.getServerUrl(), /* nThreads*/ 5);
        } else {
            graph = null;
            client = null;
        }
    }

    @Override
    public OutputStream decorate(OutputStream out) throws IOException, InterruptedException {
        if (graph != null) {
            FlowStep step = graph.poll();
            if (step != null) {
                return new FlowStepOutputStream(out, step, client);
            }
        }
        return out;
    }

    @Override
    public void onNewHead(FlowNode node) {
        if (graph != null) {
            graph.offer(node, client);
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

    /**
     * TaskListenerDecorator factory.
     */
    @SuppressRestrictedWarnings({TaskListenerDecorator.Factory.class, TaskListenerDecorator.class})
    @Extension
    public static final class Factory implements TaskListenerDecorator.Factory {

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
                if (decorator.graph != null) {
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
            synchronized (DECORATORS) {
                WeakReference<TaskListenerDecorator> ref = DECORATORS.remove(execution);
                if (ref != null) {
                    TaskListenerDecorator decorator = ref.get();
                    if (decorator != null) {
                        return;
                    }
                }
            }
            // force the result, just in case the decorator got gced.
            WorkflowRun run = FlowHelper.getRun(execution.getOwner());
            Result result = run.getResult();
            // TODO force the result
            System.out.println("TODO force push result: " + run.getFullDisplayName() + ", result: " + result);
        }

        @Override
        public void onResumed(FlowExecution execution) {
        }

        @Override
        public void onRunning(FlowExecution execution) {
        }
    }
}
