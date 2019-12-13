package io.helidon.build.publisher.model;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.build.publisher.model.Status.State;
import io.helidon.build.publisher.model.events.ArtifactsInfoEvent;
import io.helidon.build.publisher.model.events.NodeCompletedEvent;
import io.helidon.build.publisher.model.events.PipelineEvent;
import io.helidon.build.publisher.model.events.PipelineEventType;
import io.helidon.build.publisher.model.events.StageCreatedEvent;
import io.helidon.build.publisher.model.events.StepCreatedEvent;
import io.helidon.build.publisher.model.events.TestsInfoEvent;

import static io.helidon.build.publisher.model.Stage.StageType.PARALLEL;
import static io.helidon.build.publisher.model.Stage.StageType.SEQUENCE;
import static io.helidon.build.publisher.model.Stage.StageType.STEPS;
import static io.helidon.build.publisher.model.events.PipelineEventType.ARTIFACTS_INFO;
import static io.helidon.build.publisher.model.events.PipelineEventType.PIPELINE_COMPLETED;
import static io.helidon.build.publisher.model.events.PipelineEventType.STAGE_COMPLETED;
import static io.helidon.build.publisher.model.events.PipelineEventType.STAGE_CREATED;
import static io.helidon.build.publisher.model.events.PipelineEventType.STEP_COMPLETED;
import static io.helidon.build.publisher.model.events.PipelineEventType.STEP_CREATED;
import static io.helidon.build.publisher.model.events.PipelineEventType.TESTS_INFO;

/**
 * Pipeline event processor.
 */
public final class PipelineEventProcessor {

    private static final Logger LOGGER = Logger.getLogger(PipelineEventProcessor.class.getName());

    private final Pipeline pipeline;

    /**
     * Create a new processor.
     * @param pipeline the pipeline
     */
    public PipelineEventProcessor(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * Process the given events.
     * @param events the events to apply
     */
    public void process(List<PipelineEvent> events) {
        for (PipelineEvent event : events) {
            PipelineEventType eventType = event.eventType();
            if (pipeline.status.state == State.FINISHED && eventType != PipelineEventType.PIPELINE_COMPLETED) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Skipping event, pipeline is in FINISHED state: {0}", event);
                }
                continue;
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Applying event: {0}", event);
            }
            switch (eventType) {
                case ARTIFACTS_INFO:
                    processArtifactsInfoEvent((ArtifactsInfoEvent) event);
                    break;
                case TESTS_INFO:
                    processTestsEvent((TestsInfoEvent) event);
                    break;
                case PIPELINE_COMPLETED:
                    processPipelineCompletedEvent();
                    break;
                case STEP_COMPLETED:
                case STAGE_COMPLETED:
                    processNodeCompletedEvent((NodeCompletedEvent) event);
                    break;
                case STEP_CREATED:
                    processStepCreatedEvent((StepCreatedEvent) event);
                    break;
                case STAGE_CREATED:
                    processStageCreatedEvent((StageCreatedEvent) event);
                    break;
                default:
                // do nothing
            }
        }
    }

    private Node getNode(int id) {
        Node node = pipeline.nodesByIds.get(id);
        if (node == null) {
            throw new IllegalStateException("Unkown node, id=" + id);
        }
        return node;
    }

    private Steps getSteps(int id) {
        Node node = getNode(id);
        if (!(node instanceof Steps)) {
            throw new IllegalStateException("Invalid steps node, id=" + id);
        }
        return (Steps) node;
    }

    private Stages getStages(int id) {
        Node node = getNode(id);
        if (!(node instanceof Stages)) {
            throw new IllegalStateException("Invalid stages node, id=" + id);
        }
        return (Stages) node;
    }

    private void processArtifactsInfoEvent(ArtifactsInfoEvent event) {
        getSteps(event.stepsId()).artifacts = event.count();
    }

    private void processTestsEvent(TestsInfoEvent event) {
        getSteps(event.stepsId()).tests = event.info();
    }

    private void processPipelineCompletedEvent() {
        if (pipeline.status.state != State.FINISHED) {
            pipeline.status.state = State.FINISHED;
            pipeline.status.result = Status.Result.UNKNOWN;
        }
    }

    private void processNodeCompletedEvent(NodeCompletedEvent event) {
        Node node = getNode(event.id());
        node.status.state = State.FINISHED;
        node.status.result = event.result();
        node.timings.endTime = event.endTime();
    }

    private void processStepCreatedEvent(StepCreatedEvent event) {
        Steps steps = getSteps(event.id());
        if (event.index() != steps.children.size()) {
            throw new IllegalStateException("Invalid index");
        }
        steps.addStep(new Step(event.id(), steps, event.name(), event.args(), event.meta(), event.declared(), new Status(),
                new Timings(event.startTime())));
    }

    private void processStageCreatedEvent(StageCreatedEvent event) {
        Stages stages = getStages(event.id());
        if (event.index() != stages.children.size()) {
            throw new IllegalStateException("Invalid index");
        }
        switch (event.stageType()) {
            case PARALLEL:
                stages.addStage(new Parallel(event.id(), stages, event.name(), new Status(), new Timings(event.startTime())));
                break;
            case SEQUENCE:
                stages.addStage(new Sequence(event.id(), stages, event.name(), new Status(), new Timings(event.startTime())));
                break;
            case STEPS:
                stages.addStage(new Steps(event.id(), stages, new Status(), new Timings(event.startTime())));
                break;
            default:
                // do nothing
        }
    }
}
