package io.helidon.jenkins.publisher;

import hudson.Extension;
import hudson.console.ConsoleNote;
import hudson.console.LineTransformationOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.jenkinsci.plugins.workflow.actions.ArgumentsAction;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.ThreadNameAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.log.TaskListenerDecorator;
import org.kohsuke.accmod.restrictions.suppressions.SuppressRestrictedWarnings;

/**
 * Entry point to intercept flow steps console output.
 */
@SuppressRestrictedWarnings({TaskListenerDecorator.Factory.class, TaskListenerDecorator.class})
@Extension
public class FlowDecoratorFactory implements TaskListenerDecorator.Factory {

    private static final EmptyDecorator EMPTY_DECORATOR = new EmptyDecorator();
    private static final Map<FlowExecution, WeakReference<FlowLogDecorator>> DECORATORS = new WeakHashMap<>();

    /**
     * Remove the decorator associated with the given execution.
     * @param exec the flow execution for which remove the cached decorator
     */
    static void clear(FlowExecution exec) {
        DECORATORS.remove(exec);
    }

    @Override
    public TaskListenerDecorator of(FlowExecutionOwner owner) {
        FlowExecution execution = owner.getOrNull();
        if (execution == null) {
            return EMPTY_DECORATOR;
        }
        synchronized (DECORATORS) {
            WeakReference<FlowLogDecorator> decoratorRef = DECORATORS.get(execution);
            if (decoratorRef != null && decoratorRef.get() != null) {
                return decoratorRef.get();
            }
        }
        FlowLogDecorator decorator = new FlowLogDecorator(execution);
        execution.addListener(decorator);
        synchronized (DECORATORS) {
            WeakReference<FlowLogDecorator> decoratorRef = DECORATORS.get(execution);
            if (decoratorRef != null && decoratorRef.get() != null) {
                return decoratorRef.get();
            }
            DECORATORS.put(execution, new WeakReference<>(decorator));
            return decorator;
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
     * Decorator that matches node head with console output and selectively decorates it.
     */
    @SuppressRestrictedWarnings(TaskListenerDecorator.class)
    private static final class FlowLogDecorator extends TaskListenerDecorator implements GraphListener.Synchronous {

        private final LinkedList<StepNodeWrapper> headNodes = new LinkedList<>();
        private final FlowStepSignatures stepsSignatures;

        FlowLogDecorator(FlowExecution execution) {
            this.stepsSignatures = FlowStepSignatures.getOrCreate(execution);
        }

        @Override
        public OutputStream decorate(OutputStream out) throws IOException, InterruptedException {
            if (!headNodes.isEmpty()) {
                StepNodeWrapper node = headNodes.pop();
                return new StepOutputStream(out, node);
            }
            return out;
        }

        @Override
        public void onNewHead(FlowNode node) {
            System.out.println("node: " + node.getId() + ", " + node.getDisplayName() + ", " + ArgumentsAction.getStepArgumentsAsString(node));
            if ((node instanceof StepAtomNode)) {
                this.headNodes.addLast(new StepNodeWrapper((StepAtomNode)node, stepsSignatures.contains((StepAtomNode) node)));
            }
        }
    }

    /**
     * Step node Wrapper.
     */
    private static final class StepNodeWrapper {

        private final StepAtomNode node;
        private final StepStartNode stageNode;
        private final String id;
        private final String stepName;
        private final String stageId;
        private final String stageName;

        StepNodeWrapper(StepAtomNode node, boolean declarative) {
            StepStartNode stageParent = null;
            Iterator<? extends BlockStartNode> it = node.getEnclosingBlocks().iterator();
            while(it.hasNext()) {
                BlockStartNode parentNode = it.next();
                if (parentNode instanceof StepStartNode) {
                    if (isStage(parentNode)) {
                        stageParent = (StepStartNode) parentNode;
                    }
                }
            }
            this.node = node;
            this.id = node.getId();
            this.stepName = node.getDisplayFunctionName();
            this.stageNode = stageParent;
            this.stageId = stageNode != null ? stageNode.getId() : "0";
            this.stageName = stageNode != null ? stageNode.getDisplayName() : "stage" + stageId;
        }

        String getId() {
            return id;
        }

        String getStageId() {
            return stageId;
        }

        String getStageName() {
            return stageName;
        }

        String getStepName() {
            return stepName;
        }

        StepAtomNode getNode() {
            return node;
        }

        StepStartNode getStageNode() {
            return stageNode;
        }

        @Override
        public String toString() {
            return StepNodeWrapper.class.getSimpleName() + "{ "
                    + "id=" + id
                    + " ,stepName=" + stepName
                    + " ,stageId=" + stageId
                    + " ,stageName=" + stageName
                    + "}";
        }

        /**
         * Test if the given node is a stage.
         * @param node node to test
         * @return {@code true} if a stage, {@code false} otherwise
         */
        private static boolean isStage(FlowNode node) {
            return node != null && node.getAction(LabelAction.class) != null && node.getAction(ThreadNameAction.class) == null;
        }
    }

    /**
     * OutputStream wrapper to intercept step output.
     */
    private static final class StepOutputStream extends LineTransformationOutputStream {

        private static final AtomicInteger IDS = new AtomicInteger();
        private final OutputStream out;
        private final StepNodeWrapper node;
        private final int id;

        /**
         * Create a new instance.
         * @param out the stream to wrap
         * @param node the associated step
         */
        StepOutputStream(OutputStream out, StepNodeWrapper node) {
            super();
            this.out = out;
            this.node = node;
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
                    + " ,node=" + node.toString()
                    + "}";
        }
    }
}
