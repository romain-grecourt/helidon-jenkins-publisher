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
    private static final Map<FlowExecution, WeakReference<FlowDecorator>> DECORATORS = new WeakHashMap<>();

    /**
     * Remove the decorator associated with the given execution.
     * @param exec the flow execution for which remove the cached decorator
     */
    static void clear(FlowExecution exec) {
        synchronized (DECORATORS) {
            DECORATORS.remove(exec);
        }
    }

    @Override
    public TaskListenerDecorator of(FlowExecutionOwner owner) {
        FlowExecution execution = owner.getOrNull();
        if (execution == null) {
            return EMPTY_DECORATOR;
        }
        synchronized (DECORATORS) {
            WeakReference<FlowDecorator> decoratorRef = DECORATORS.get(execution);
            if (decoratorRef != null && decoratorRef.get() != null) {
                return decoratorRef.get();
            }
        }
        FlowDecorator decorator = new FlowDecorator(execution);
        execution.addListener(decorator);
        synchronized (DECORATORS) {
            WeakReference<FlowDecorator> decoratorRef = DECORATORS.get(execution);
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
}
