package io.helidon.build.publisher.model;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.build.publisher.model.Status.State;
import io.helidon.build.publisher.model.events.ArtifactsInfoEvent;
import io.helidon.build.publisher.model.events.NodeCompletedEvent;
import io.helidon.build.publisher.model.events.PipelineEvent;
import io.helidon.build.publisher.model.events.PipelineCreatedEvent;
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

    private final PipelineDescriptorManager manager;

    /**
     * Create a new processor.
     * @param manager
     */
    public PipelineEventProcessor(PipelineDescriptorManager manager) {
        this.manager = manager;
    }

    /**
     * Process the given mixed events that may belong to different pipelines.
     * @param allEvents events to process
     * @throws IllegalStateException if unable to get a pipeline descriptor
     */
    public void process(List<PipelineEvent> allEvents) {
        Pipeline pipeline = null;
        List<PipelineEvent> events = new LinkedList<>();
        for (PipelineEvent event : allEvents) {
            String epid = event.pipelineId();
            if (pipeline == null || !pipeline.info.id.equals(epid)) {
                if (pipeline != null) {
                    process(pipeline, events);
                    manager.save(pipeline);
                    events = new LinkedList<>();
                }
                pipeline = manager.load(epid);
                if (pipeline == null) {
                    if (event.eventType() == PipelineEventType.PIPELINE_CREATED) {
                        PipelineCreatedEvent createdEvent = (PipelineCreatedEvent) event;
                        pipeline = new Pipeline(createdEvent.info(), createdEvent.startTime());
                    } else {
                        throw new IllegalStateException("Unable to get pipeline descriptor, pipelineId=" + epid);
                    }
                }
            }
            events.add(event);
        }
        if (pipeline != null) {
            process(pipeline, events);
            manager.save(pipeline);
        }
    }

    /**
     * Process the given events.
     * @param events the events to apply
     */
    private static void process(Pipeline pipeline, List<PipelineEvent> events) {
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
                    processArtifactsInfoEvent(pipeline, (ArtifactsInfoEvent) event);
                    break;
                case TESTS_INFO:
                    processTestsEvent(pipeline, (TestsInfoEvent) event);
                    break;
                case PIPELINE_COMPLETED:
                    processPipelineCompletedEvent(pipeline);
                    break;
                case STEP_COMPLETED:
                case STAGE_COMPLETED:
                    processNodeCompletedEvent(pipeline, (NodeCompletedEvent) event);
                    break;
                case STEP_CREATED:
                    processStepCreatedEvent(pipeline, (StepCreatedEvent) event);
                    break;
                case STAGE_CREATED:
                    processStageCreatedEvent(pipeline, (StageCreatedEvent) event);
                    break;
                default:
                // do nothing
            }
        }
    }

    private static Node getNode(Pipeline pipeline, int id) {
        Node node = pipeline.nodesByIds.get(id);
        if (node == null) {
            throw new IllegalStateException("Unkown node, id=" + id);
        }
        return node;
    }

    private static Steps getSteps(Pipeline pipeline, int id) {
        Node node = getNode(pipeline, id);
        if (!(node instanceof Steps)) {
            throw new IllegalStateException("Invalid steps node, id=" + id);
        }
        return (Steps) node;
    }

    private static Stages getStages(Pipeline pipeline, int id) {
        Node node = getNode(pipeline, id);
        if (!(node instanceof Stages)) {
            throw new IllegalStateException("Invalid stages node, id=" + id);
        }
        return (Stages) node;
    }

    private static void processArtifactsInfoEvent(Pipeline pipeline, ArtifactsInfoEvent event) {
        getSteps(pipeline, event.stepsId()).artifacts = event.count();
    }

    private static void processTestsEvent(Pipeline pipeline, TestsInfoEvent event) {
        getSteps(pipeline, event.stepsId()).tests = event.info();
    }

    private static void processPipelineCompletedEvent(Pipeline pipeline) {
        if (pipeline.status.state != State.FINISHED) {
            pipeline.status.state = State.FINISHED;
            pipeline.status.result = Status.Result.UNKNOWN;
        }
    }

    private static void processNodeCompletedEvent(Pipeline pipeline, NodeCompletedEvent event) {
        Node node = getNode(pipeline, event.id());
        node.status.state = State.FINISHED;
        node.status.result = event.result();
        node.timings.endTime = event.endTime();
    }

    private static void processStepCreatedEvent(Pipeline pipeline, StepCreatedEvent event) {
        Steps steps = getSteps(pipeline, event.parentId());
        if (event.index() != steps.children.size()) {
            throw new IllegalStateException("Invalid index");
        }
        steps.addStep(new Step(event.id(), steps, event.name(), event.args(), false, true, new Status(),
            new Timings(event.startTime())));
    }

    private static void processStageCreatedEvent(Pipeline pipeline, StageCreatedEvent event) {
        Stages stages = getStages(pipeline, event.parentId());
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
