package io.helidon.jenkins.publisher;

import hudson.Extension;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.log.TaskListenerDecorator;
import org.kohsuke.accmod.restrictions.suppressions.SuppressRestrictedWarnings;

/**
 * Entry point to intercept flow steps console output.
 */
@SuppressRestrictedWarnings({TaskListenerDecorator.Factory.class, TaskListenerDecorator.class})
@Extension
public class FlowDecoratorFactory implements TaskListenerDecorator.Factory {

    private static final EmptyDecorator EMPTY_DECORATOR = new EmptyDecorator();
    private static final Map<FlowExecution, WeakReference<TaskListenerDecorator>> DECORATORS = new WeakHashMap<>();

    /**
     * Remove the decorator associated with the given execution.
     * @param exec the flow execution for which remove the cached decorator
     * @return the removed decorator
     */
    static FlowDecorator clear(FlowExecution exec) {
        synchronized (DECORATORS) {
            WeakReference<TaskListenerDecorator> ref = DECORATORS.remove(exec);
            if (ref != null) {
                TaskListenerDecorator decorator = ref.get();
                if (decorator instanceof FlowDecorator) {
                    return (FlowDecorator) decorator;
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
        FlowDecorator decorator = new FlowDecorator(execution);
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
}
