package io.helidon.jenkins.publisher.plugin;

import java.util.HashMap;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

/**
 * Flow event.
 */
abstract class FlowEvent {

    private static final JsonBuilderFactory JSON_BUILDER_FACTORY = Json.createBuilderFactory(new HashMap<>());

    /**
     * Event type.
     */
    enum Type {

        /**
         * A new step has been created and is active.
         */
        STEP_CREATED,

        /**
         * A step has been completed.
         */
        STEP_COMPLETED,

        /**
         * A new stage has been created.
         */
        STAGE_CREATED,

        /**
         * A stage has been completed.
         */
        STAGE_COMPLETED,

        /**
         * Output has been produced.
         */
        OUTPUT,

        /**
         * A job / flow has been created.
         */
        CREATED,

        /**
         * A job / flow has been completed.
         */
        COMPLETED,

        /**
         * Error event.
         */
        ERROR
    }

    /**
     * Event listener.
     */
    interface Listener {

        /**
         * Process a flow event.
         * @param event the event to process
         */
        void onEvent(FlowEvent event);
    }

    /**
     * Step event.
     */
    static final class StepEvent extends FlowEvent {

        private final FlowStep step;
        private final boolean completed;

        StepEvent(FlowStep step, boolean completed) {
            this.step = step;
            this.completed = completed;
        }

        FlowStep step() {
            return step;
        }

        @Override
        Type type() {
            return completed ? Type.STEP_COMPLETED : Type.STEP_CREATED;
        }

        @Override
        FlowRun flowRun() {
            return step.flowRun();
        }

        @Override
        public String toString() {
            return StepEvent.class.getSimpleName() + "{"
                    + " step=" + step.toString()
                    + " ,completed=" + completed
                    + " }";
        }

        @Override
        JsonObject toJson() {
            return JSON_BUILDER_FACTORY.createObjectBuilder()
                    .add("type", "step")
                    .add("completed", completed)
                    .add("step", step.toJson())
                    .build();
        }
    }

    /**
     * Stage event.
     */
    static final class StageEvent extends FlowEvent {

        private final FlowStage stage;
        private final boolean completed;

        StageEvent(FlowStage stage, boolean completed) {
            this.stage = stage;
            this.completed = completed;
        }

        FlowStage stage() {
            return stage;
        }

        @Override
        Type type() {
            return completed ? Type.STAGE_COMPLETED : Type.STAGE_CREATED;
        }

        @Override
        FlowRun flowRun() {
            return stage.flowRun();
        }

        @Override
        public String toString() {
            return StageEvent.class.getSimpleName() + "{"
                    + " stage=" + stage.toString()
                    + " ,completed=" + completed
                    + " }";
        }

        @Override
        JsonObject toJson() {
            return JSON_BUILDER_FACTORY.createObjectBuilder()
                    .add("type", "stage")
                    .add("completed", completed)
                    .add("stage", stage.toJson())
                    .build();
        }
    }

    /**
     * Output event.
     */
    static final class OutputEvent extends FlowEvent {

        private final FlowStep step;
        private final byte[] data;

        OutputEvent(FlowStep step, byte[] data) {
            this.step = step;
            this.data = data;
        }

        FlowStep step() {
            return step;
        }

        byte[] data() {
            return data;
        }

        @Override
        Type type() {
            return Type.OUTPUT;
        }

        @Override
        FlowRun flowRun() {
            return step.flowRun();
        }

        @Override
        public String toString() {
            return OutputEvent.class.getSimpleName() + "{"
                    + " step=" + step.toString()
                    + " ,data= " + new String(data)
                    + " }";
        }

        @Override
        JsonObject toJson() {
            return JSON_BUILDER_FACTORY.createObjectBuilder()
                    .add("type", "output")
                    .add("step", step.toJson())
                    .add("data", new String(data))
                    .build();
        }
    }

    /**
     * Global event.
     */
    static final class GlobalEvent extends FlowEvent {

        private final FlowStage.Sequence root;
        private final boolean completed;

        GlobalEvent(FlowStage.Sequence root, boolean completed) {
            this.root = root;
            this.completed = completed;
        }

        FlowStage.Sequence root() {
            return root;
        }

        @Override
        Type type() {
            return completed ? Type.COMPLETED : Type.CREATED;
        }

        @Override
        FlowRun flowRun() {
            return root.flowRun();
        }

        @Override
        public String toString() {
            return GlobalEvent.class.getSimpleName() + "{"
                    + " root=" + root.toString()
                    + " ,completed=" + completed
                    + " }";
        }

        @Override
        JsonObject toJson() {
            return JSON_BUILDER_FACTORY.createObjectBuilder()
                    .add("type", "global")
                    .add("completed", completed)
                    .build();
        }
    }

    /**
     * Error event.
     */
    static final class ErrorEvent extends FlowEvent {

        private final FlowRun flowRun;
        private final int code;
        private final String message;

        ErrorEvent(FlowRun flowRun, int code, String message) {
            this.flowRun = flowRun;
            this.code = code;
            this.message = message;
        }

        @Override
        Type type() {
            return Type.ERROR;
        }

        @Override
        FlowRun flowRun() {
            return flowRun;
        }

        @Override
        public String toString() {
            return ErrorEvent.class.getSimpleName() + "{"
                    + " flowRun=" + flowRun.toString()
                    + " ,code=" + code
                    + " ,message=" + message
                    + " }";
        }

        @Override
        JsonObject toJson() {
            return JSON_BUILDER_FACTORY.createObjectBuilder()
                    .add("type", "error")
                    .add("code", code)
                    .add("message", message)
                    .build();
        }
    }

    /**
     * Get the event type.
     *
     * @return EventType, never {@code null}
     */
    abstract Type type();

    /**
     * Get the flow run.
     * @return FlowRun
     */
    abstract FlowRun flowRun();

    /**
     * Get this event as a {@link GlobalEvent}
     * @return GlobalEvent
     * @throws IllegalStateException if this event is not a {@link GlobalEvent}
     */
    GlobalEvent asGlobal() {
        if (this instanceof GlobalEvent) {
            return (GlobalEvent) this;
        }
        throw new IllegalStateException("Not a GlobalEvent instance");
    }

    /**
     * Get this event as a {@link OutputEvent}
     * @return OutputEvent
     * @throws IllegalStateException if this event is not a {@link OutputEvent}
     */
    OutputEvent asOutput() {
        if (this instanceof OutputEvent) {
            return (OutputEvent) this;
        }
        throw new IllegalStateException("Not a OutputEvent instance");
    }

    /**
     * Get this event as a {@link StageEvent}
     * @return StageEvent
     * @throws IllegalStateException if this event is not a {@link StageEvent}
     */
    StageEvent asStage() {
        if (this instanceof StageEvent) {
            return (StageEvent) this;
        }
        throw new IllegalStateException("Not a StageEvent instance");
    }

    /**
     * Get this event as a {@link StepEvent}
     * @return StepEvent
     * @throws IllegalStateException if this event is not a {@link StepEvent}
     */
    StepEvent asStep() {
        if (this instanceof StepEvent) {
            return (StepEvent) this;
        }
        throw new IllegalStateException("Not a StepEvent instance");
    }

    /**
     * Get this event as a {@link ErrorEvent}
     * @return ErrorEvent
     * @throws IllegalStateException if this event is not a {@link ErrorEvent}
     */
    ErrorEvent asError() {
        if (this instanceof ErrorEvent) {
            return (ErrorEvent) this;
        }
        throw new IllegalStateException("Not a ErrorEvent instance");
    }

    /**
     * Build a JSON object representation of this event.
     * @return JsonObject
     */
    abstract JsonObject toJson();
}
