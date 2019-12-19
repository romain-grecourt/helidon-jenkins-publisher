package io.helidon.build.publisher.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.build.publisher.model.Status.Result;
import io.helidon.build.publisher.model.Status.State;
import io.helidon.build.publisher.model.events.ArtifactsInfoEvent;
import io.helidon.build.publisher.model.events.NodeCompletedEvent;
import io.helidon.build.publisher.model.events.PipelineCompletedEvent;
import io.helidon.build.publisher.model.events.PipelineEvent;
import io.helidon.build.publisher.model.events.PipelineCreatedEvent;
import io.helidon.build.publisher.model.events.PipelineErrorEvent;
import io.helidon.build.publisher.model.events.PipelineEventType;
import io.helidon.build.publisher.model.events.StageCreatedEvent;
import io.helidon.build.publisher.model.events.StepCreatedEvent;
import io.helidon.build.publisher.model.events.TestsInfoEvent;

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
public final class EventProcessor {

    private static final Logger LOGGER = Logger.getLogger(EventProcessor.class.getName());

    static final String UNKNOWN_ERROR = "Pipeline failed with an unkown error";
    static final String UNKNOWN_TEST_FAILURES = "Pipeline test failures are unknown";

    private final DescriptorManager manager;
    private final List<InfoAugmenter> augmenters;

    /**
     * Create a new processor.
     * @param manager descriptor manager
     * @param augmenters pipeline info augmenters, may be {@code null}
     */
    public EventProcessor(DescriptorManager manager, List<InfoAugmenter> augmenters) {
        this.manager = Objects.requireNonNull(manager, "manager is null!");
        this.augmenters = augmenters == null ? Collections.emptyList() :augmenters;
    }

    /**
     * Process the given mixed events that may belong to different pipelines.
     * @param allEvents events to process
     * @throws IllegalStateException if unable to get a pipeline descriptor
     */
    public void process(List<PipelineEvent> allEvents) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Processing events: {0}", allEvents);
        }
        Pipeline pipeline = null;
        List<PipelineEvent> events = new LinkedList<>();
        for (PipelineEvent event : allEvents) {
            String epid = event.pipelineId();
            if (pipeline == null || !pipeline.pipelineId().equals(epid)) {
                if (pipeline != null) {
                    process(pipeline, events);
                    manager.savePipeline(pipeline);
                    events = new LinkedList<>();
                }
                pipeline = manager.loadPipeline(epid);
                if (pipeline == null) {
                    if (event.eventType() == PipelineEventType.PIPELINE_CREATED) {
                        PipelineInfo info = ((PipelineCreatedEvent) event).info();
                        for (InfoAugmenter augmenter : augmenters) {
                            if (augmenter.process(info)) {
                                break;
                            }
                        }
                        pipeline = new Pipeline(info);
                    } else {
                        throw new IllegalStateException("Unable to get pipeline descriptor, pipelineId=" + epid);
                    }
                }
            }
            events.add(event);
        }
        if (pipeline != null) {
            process(pipeline, events);
            manager.savePipeline(pipeline);
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
                    processPipelineCompletedEvent(pipeline, (PipelineCompletedEvent) event);
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
                case PIPELINE_ERROR:
                    processPipelineErrorEvent(pipeline, (PipelineErrorEvent) event);
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Unprocessed event: {0}", event);
            }
        }
    }

    private static Node getNode(Pipeline pipeline, String id) {
        Node node = pipeline.nodesByIds.get(id);
        if (node == null) {
            throw new IllegalStateException("Unkown node, id=" + id);
        }
        return node;
    }

    private static Steps getSteps(Pipeline pipeline, String id) {
        Node node = getNode(pipeline, id);
        if (!(node instanceof Steps)) {
            throw new IllegalStateException("Invalid steps node, id=" + id);
        }
        return (Steps) node;
    }

    private static Stages getStages(Pipeline pipeline, String id) {
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

    private static void processPipelineCompletedEvent(Pipeline pipeline, PipelineCompletedEvent event) {
        if (pipeline.status.state != State.FINISHED) {
            pipeline.status.state = State.FINISHED;
            pipeline.status.result = event.result();
            pipeline.timings.duration(event.duration());
            pipeline.visit(new CompletedVisitor());
        }
    }

    private static void processPipelineErrorEvent(Pipeline pipeline, PipelineErrorEvent event) {
        if (pipeline.status.state != State.FINISHED) {
            pipeline.status.state = State.FINISHED;
            pipeline.status.result = Result.ABORTED;
            pipeline.timings.endTime = System.currentTimeMillis();
            String error;
            String message = event.message();
            if (message != null) {
                error = message;
                int code = event.code();
                if (code > 0) {
                    error += " (" + code + ")";
                }
            } else {
                error = null;
            }
            pipeline.visit(new CompletedVisitor(error));
        }
    }

    private static void processNodeCompletedEvent(Pipeline pipeline, NodeCompletedEvent event) {
        Node node = getNode(pipeline, event.id());
        node.status.state = State.FINISHED;
        node.status.result = event.result();
        node.timings.duration(event.duration());
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
            case "PARALLEL":
                stages.addStage(new Parallel(stages, event.id(), event.name(), new Status(), new Timings(event.startTime())));
                break;
            case "SEQUENCE":
                stages.addStage(new Sequence(stages, event.id(), event.name(), new Status(), new Timings(event.startTime())));
                break;
            case "STEPS":
                stages.addStage(new Steps(stages, event.id(), new Status(), new Timings(event.startTime())));
                break;
            default:
                LOGGER.log(Level.WARNING, "Unprocessed stage created event: {0}", event);
        }
    }

    /**
     * A pipeline visitor that ensure the visited pipeline is fully completed.
     */
    private static final class CompletedVisitor implements PipelineVisitor {

        final String error;
        boolean foundUnstable;
        boolean foundFailure;
        Result result;

        CompletedVisitor() {
            this.error = null;
        }

        CompletedVisitor(String error) {
            this.error = error;
        }

        @Override
        public void visitStart(Pipeline pipeline) {
            result = pipeline.status.result;
        }

        @Override
        public void visitStagesStart(Stages stages, int depth) {
        }

        @Override
        public void visitStepsStart(Steps steps, int depth) {
        }

        @Override
        public void visitStep(Step step, int depth) {
            ensureCompleted(step);
            if (Result.FAILURE == result && !foundFailure && step.status.result == Result.FAILURE) {
                foundFailure = true;
            }
        }

        @Override
        public void visitStepsEnd(Steps steps, int depth) {
            if (steps.children.isEmpty()) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Removing empty steps, steps={0}", steps);
                }
                ((Stages)steps.parent).children.remove(steps.index());
            } else {
                ensureCompleted(steps);
                if (Status.Result.UNSTABLE == result && steps.tests != null && steps.tests.failed > 0) {
                    steps.status.result = Result.UNSTABLE;
                    foundUnstable = true;
                }
            }
        }

        @Override
        public void visitStagesEnd(Stages stages, int depth) {
            if (stages.children.isEmpty()) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Removing empty stages, steps={0}", stages);
                }
                ((Stages)stages.parent).children.remove(stages.index());
            } else {
                ensureCompleted(stages);
            }
        }

        @Override
        public void visitEnd(Pipeline pipeline) {
            if (Result.FAILURE == result && !foundFailure) {
                LOGGER.log(Level.WARNING, "Pipeline failed with an unkown error, pipelineId={0}", pipeline.info.id);
                pipeline.error = UNKNOWN_ERROR;
            } else if (Result.UNSTABLE == result && !foundUnstable) {
                LOGGER.log(Level.WARNING, "Pipeline test failures are unknown, pipelineId={0}", pipeline.info.id);
                pipeline.error = UNKNOWN_TEST_FAILURES;
            } else if (Result.ABORTED == result && error != null) {
                LOGGER.log(Level.WARNING, "Pipeline aborted, error={0}, pipelineId={1}", new Object[]{
                    error,
                    pipeline.info.id
                });
                pipeline.error = error;
            }
        }

        private void ensureCompleted(Node node) {
            if (node.status.state == State.RUNNING) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Forcing completion, node={0}", node);
                }
                node.fireCompleted();
                if (result == Result.ABORTED) {
                    // pipeline is aborted, always set the node result to aborted
                    node.status.result = Result.ABORTED;
                } else if (result == Result.SUCCESS && node.status.result == Result.UNKNOWN) {
                    // pipeline is a success and node result is unknown
                    // force the node result to success
                    node.status.result = Result.SUCCESS;
                }
            }
        }
    }
}
