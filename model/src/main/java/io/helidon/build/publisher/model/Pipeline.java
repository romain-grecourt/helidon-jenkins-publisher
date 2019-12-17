package io.helidon.build.publisher.model;

import java.util.List;
import java.util.LinkedList;

import io.helidon.build.publisher.model.events.PipelineEventListener;
import io.helidon.build.publisher.model.events.PipelineCompletedEvent;
import io.helidon.build.publisher.model.events.PipelineCreatedEvent;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Pipeline model.
 */
@JsonDeserialize(using = PipelineDeserializer.class)
@JsonSerialize(using = PipelineSerializer.class)
public final class Pipeline extends Stages {

    /**
     * Create a new running pipeline.
     * @param info pipeline info
     * @throws NullPointerException if info is {@code null}
     */
    public Pipeline(PipelineInfo info) {
        super(info);
    }

    /**
     * Create a new running pipeline from a {@link PipelineCreatedEvent} event.
     * @param event created event
     */
    public Pipeline(PipelineCreatedEvent event) {
        this(event.info());
    }

    /**
     * Add an event listener.
     * @param listener listener to register
     */
    public void addEventListener(PipelineEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Get a node by id.
     * @param id node id
     * @return {@link Node} or {@code null} if not found
     */
    public Node node(String id) {
        return nodesByIds.get(id);
    }

    @Override
    public StageType type() {
        return StageType.SEQUENCE;
    }

    @Override
    public void fireCompleted() {
        Stage last = null;
        if (!children.isEmpty()) {
            last = children.get(children.size() - 1);
            last.fireCompleted();
        }
        status.state = Status.State.FINISHED;
        status.result = last != null ? last.result() : Status.Result.SUCCESS;
        timings.endTime = last != null ? last.endTime() : System.currentTimeMillis();
        fireEvent(new PipelineCompletedEvent(info.id, status.result, timings.endTime));
    }

    @Override
    public void fireCreated() {
        fireEvent(new PipelineCreatedEvent(info));
    }

    /**
     * Perform a depth first traversal of the pipeline.
     * @param visitor pipeline visitor
     */
    public void visit(PipelineVisitor visitor) {
        LinkedList<Stage> stack = new LinkedList<>(children);
        String parentId = null;
        int depth = 1;
        visitor.visitStart(this);
        while (!stack.isEmpty()) {
            Stage stage = stack.peek();
            if (stage instanceof Steps) {
                // leaf
                visitor.visitStepsStart((Steps) stage, depth + 1);
                for (Step step : ((Steps) stage).children) {
                    visitor.visitStep(step, depth + 2);
                }
                visitor.visitStepsEnd((Steps) stage, depth + 1);
                parentId = stage.parent.id;
                stack.pop();
            } else if (stage instanceof Stages) {
                // node
                if (stage.id.equals(parentId)) {
                    // leaving (2nd pass)
                    visitor.visitStagesEnd((Stages) stage, depth);
                    parentId = stage.parent.id;
                    stack.pop();
                    depth--;
                } else {
                    // entering
                    visitor.visitStagesStart((Stages) stage, depth);
                    List<Stage> stages = ((Stages) stage).children;
                    if (!stages.isEmpty()) {
                        // process children
                        for (int i = stages.size() - 1; i >= 0; i--) {
                            stack.push(stages.get(i));
                        }
                        depth++;
                    } else {
                        // one pass only
                        visitor.visitStagesEnd((Stages) stage, depth);
                        parentId = stage.parent.id;
                        stack.pop();
                    }
                }
            }
        }
        visitor.visitEnd(this);
    }

    /**
     * Create a pretty string to display the pipeline.
     * @param excludeSyntheticSteps {@code true} to exclude synthetic steps
     * @param excludeMetaSteps {@code true} to exclude meta steps
     * @return String
     */
    public String toPrettyString(boolean excludeSyntheticSteps, boolean excludeMetaSteps) {
        PipelinePrettyPrinter printer = new PipelinePrettyPrinter(excludeSyntheticSteps, excludeMetaSteps);
        visit(printer);
        return printer.getString();
    }
}
