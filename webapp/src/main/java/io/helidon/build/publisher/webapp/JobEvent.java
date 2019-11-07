package io.helidon.build.publisher.webapp;

import java.util.Objects;
import javax.json.JsonNumber;
import javax.json.JsonObject;

/**
 * Job event.
 */
abstract class JobEvent {

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
     * Step event.
     */
    static final class StepEvent extends JobEvent {

        private final String jobId;
        private final String stepId;
        private final boolean completed;
        private final String stageId;
        private final String name;
        private final String state;
        private final String result;
        private final long startTime;
        private final long endTime;

        private StepEvent(String jobId, String stepId, boolean completed, String stageId, String name, String state,
                String result, long startTime, long endTime) {

            this.jobId = jobId;
            this.stepId = stepId;
            this.completed = completed;
            this.stageId = stageId;
            this.name = name;
            this.state = state;
            this.result = result;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        String jobId() {
            return jobId;
        }

        /**
         * Get the step id.
         * @return String
         */
        String stepId() {
            return stepId;
        }

        /**
         * Get the step stage id.
         * @return String
         */
        String stageId() {
            return stageId;
        }

        /**
         * Get the step name.
         * @return String
         */
        String name() {
            return name;
        }

        /**
         * Get the state.
         * @return String
         */
        String state() {
            return state;
        }

        /**
         * Get the step result.
         * @return String
         */
        String result() {
            return result;
        }

        /**
         * Get the start time.
         * @return long
         */
        long startTime() {
            return startTime;
        }

        /**
         * Get the end time.
         * @return long
         */
        long endTime() {
            return endTime;
        }

        @Override
        Type type() {
            return completed ? Type.STEP_COMPLETED : Type.STEP_CREATED;
        }

        @Override
        public String toString() {
            return StepEvent.class.getSimpleName() + "{"
                    + " jobId=" + jobId
                    + " ,stepId=" + stepId
                    + " ,completed=" + completed
                    + " }";
        }
    }

    /**
     * Stage event.
     */
    static final class StageEvent extends JobEvent {

        private final String jobId;
        private final String stageId;
        private final String parentId;
        private final String stageType;
        private final boolean completed;
        private final String name;
        private final String state;
        private final String result;
        private final long startTime;
        private final long endTime;

        private StageEvent(String jobId, String stageId, String parentId, String stageType, boolean completed, String name,
                String state, String result, long startTime, long endTime) {

            this.jobId = jobId;
            this.stageId = stageId;
            this.parentId = parentId;
            this.stageType = stageType;
            this.completed = completed;
            this.name = name;
            this.state = state;
            this.result = result;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        String jobId() {
            return jobId;
        }

        /**
         * Get the step stage id.
         * @return String
         */
        String stageId() {
            return stageId;
        }

        /**
         * Get the parent id.
         * @return String
         */
        String parentId() {
            return parentId;
        }

        /**
         * Get the stage type.
         * @return String
         */
        String stageType() {
            return stageType;
        }

        /**
         * Get the step name.
         * @return String
         */
        String name() {
            return name;
        }

        /**
         * Get the state.
         * @return String
         */
        String state() {
            return state;
        }

        /**
         * Get the step result.
         * @return String
         */
        String result() {
            return result;
        }

        /**
         * Get the start time.
         * @return long
         */
        long startTime() {
            return startTime;
        }

        /**
         * Get the end time.
         * @return long
         */
        long endTime() {
            return endTime;
        }

        @Override
        Type type() {
            return completed ? Type.STAGE_COMPLETED : Type.STAGE_CREATED;
        }

        @Override
        public String toString() {
            return StageEvent.class.getSimpleName() + "{"
                    + " jobId=" + jobId
                    + " stageId=" + stageId
                    + " ,completed=" + completed
                    + " }";
        }
    }

    /**
     * Output event.
     */
    static final class OutputEvent extends JobEvent {

        private final String jobId;
        private final String stepId;
        private final byte[] data;

        // TODO store lineno
        OutputEvent(String jobId, String stepId, byte[] data) {
            Objects.requireNonNull(jobId, "jobId is null");
            Objects.requireNonNull(stepId, "stepId is null");
            this.jobId = jobId;
            this.stepId = stepId;
            this.data = data;
        }

        @Override
        String jobId() {
            return jobId;
        }

        /**
         * Get the step id.
         * @return String
         */
        String stepId() {
            return stepId;
        }

        /**
         * Get the data.
         * @return byte[]
         */
        byte[] data() {
            return data;
        }

        @Override
        Type type() {
            return Type.OUTPUT;
        }

        @Override
        public String toString() {
            return OutputEvent.class.getSimpleName() + "{"
                    + " jobId=" + jobId
                    + " ,stepId=" + stepId
                    + " ,data= " + new String(data)
                    + " }";
        }
    }

    /**
     * Global event.
     */
    static final class GlobalEvent extends JobEvent {

        private final String jobId;
        private final boolean completed;

        private GlobalEvent(String jobId, boolean completed) {
            Objects.requireNonNull(jobId, "jobId is null");
            this.jobId = jobId;
            this.completed = completed;
        }

        @Override
        String jobId() {
            return jobId;
        }

        @Override
        Type type() {
            return completed ? Type.COMPLETED : Type.CREATED;
        }

        @Override
        public String toString() {
            return GlobalEvent.class.getSimpleName() + "{"
                    + " jobId=" + jobId
                    + " ,completed=" + completed
                    + " }";
        }
    }

    /**
     * Error event.
     */
    static final class ErrorEvent extends JobEvent {

        private final String jobId;
        private final int code;
        private final String message;

        private ErrorEvent(String jobId, int code, String message) {
            Objects.requireNonNull(jobId, "jobId is null");
            this.jobId = jobId;
            this.code = code;
            this.message = message;
        }

        @Override
        String jobId() {
            return jobId;
        }

        @Override
        Type type() {
            return Type.ERROR;
        }

        /**
         * Get the error code.
         * @return int
         */
        int code() {
            return code;
        }

        /**
         * Get the error message.
         * @return String
         */
        String message() {
            return message;
        }

        @Override
        public String toString() {
            return ErrorEvent.class.getSimpleName() + "{"
                    + " jobId=" + jobId
                    + " ,code=" + code
                    + " ,message=" + message
                    + " }";
        }
    }

    /**
     * Get the event type.
     *
     * @return EventType, never {@code null}
     */
    abstract Type type();

    /**
     * Get the job id.
     *
     * @return String, never {@code null}
     */
    abstract String jobId();

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
     * Create a new {@link StepEvent} from the given {@link JsonObject}.
     * @param jobId jobId that this event belongs to
     * @param step the JSON event
     * @return StepEvent
     */
    private static StepEvent createStepEvent(String jobId, JsonObject step) {
        String stepId = getString("id", step, "step");
        String stageId = getString("stageId", step, "step");
        boolean completed = getBoolean("completed", step, "step");
        String name = getString("name", step, "step");
        JsonObject status = getObject("status", step, "step");
        String state = getString("state", status, "status");
        String result = getString("result", status, "status");
        long startTime = getNumber("startTime", step, "step").longValue();
        long endTime = getNumber("endTime", step, "step").longValue();
        return new StepEvent(jobId, stepId, completed, stageId, name, state, result, startTime, endTime);
    }

    /**
     * Create a new {@link StageEvent} from the given {@link JsonObject}.
     *
     * @param jobId jobId that this event belongs to
     * @param stage the JSON event
     * @return StageEvent
     */
    private static StageEvent createStageEvent(String jobId, JsonObject stage) {
        String stageId = getString("id", stage, "stage");
        String parentId = getString("parentId", stage, "stage");
        String stageType = getString("type", stage, "stage");
        boolean completed = getBoolean("completed", stage, "stage");
        String name = getString("name", stage, "stage");
        JsonObject status = getObject("status", stage, "stage");
        String state = getString("state", status, "status");
        String result = getString("result", status, "status");
        long startTime = getNumber("startTime", stage, "stage").longValue();
        long endTime = getNumber("endTime", stage, "stage").longValue();
        return new StageEvent(jobId, stageId, parentId, stageType, completed, name, state, result, startTime, endTime);
    }

    /**
     * Create a job event from the given {@link JsonObject}.
     * @param event JSON event
     * @return JobEvent
     */
    static JobEvent fromJson(String jobId, JsonObject event) {
        String type = getString("type", event, "event");
        switch (type) {
            case "step":
                return createStepEvent(jobId, getObject("step", event, "event"));
            case "stage":
                return createStageEvent(jobId, getObject("stage", event, "event"));
            case "global":
                return new GlobalEvent(jobId, getBoolean("completed", event, "event"));
            case "error":
                return new ErrorEvent(jobId,
                        getNumber("code", event, "event").intValue(),
                        getString("message", event, "event"));
            default:
                throw new IllegalArgumentException("Not a valid JSON event");
        }
    }

    /**
     * Get a {@link JsonObject} property.
     *
     * @param key property key
     * @param json JSON object to get the property value from
     * @param errorPrefix error message prefix
     * @return JsonObject
     */
    private static JsonObject getObject(String key, JsonObject json, String errorPrefix) {
        JsonObject value;
        try {
            value = json.getJsonObject(key);
        } catch (ClassCastException ex) {
            throw new IllegalArgumentException(errorPrefix + "'" + key + "' property is not an object");
        }
        if (value == null) {
            throw new IllegalArgumentException(errorPrefix + " does not have a property '" + key + "'");
        }
        return value;
    }

    /**
     * Get a {@link String} property.
     * @param key property key
     * @param json JSON object to get the property value from
     * @param errorMessagePrefix error message prefix
     * @return String
     */
    private static String getString(String key, JsonObject json, String errorMessagePrefix) {
        String value;
        try {
            value = json.getString(key);
        } catch (ClassCastException ex) {
            throw new IllegalArgumentException(errorMessagePrefix + " " + key + " is not a string");
        }
        if (value == null) {
            throw new IllegalArgumentException(errorMessagePrefix + " does not have a property '" + key + "'");
        }
        return value;
    }

    /**
     * Get a {@link JsonNumber} property.
     * @param key property key
     * @param json JSON object to get the property value from
     * @param errorMessagePrefix error message prefix
     * @return String
     */
    private static JsonNumber getNumber(String key, JsonObject json, String errorMessagePrefix) {
        JsonNumber value;
        try {
            value = json.getJsonNumber(key);
        } catch (ClassCastException ex) {
            throw new IllegalArgumentException(errorMessagePrefix + " " + key + " is not a number");
        }
        if (value == null) {
            throw new IllegalArgumentException(errorMessagePrefix + " does not have a property '" + key + "'");
        }
        return value;
    }

    /**
     * Get a {@link boolean} property.
     * @param key property key
     * @param json JSON object to get the property value from
     * @param errorMessagePrefix error message prefix
     * @return {@code boolean}
     */
    private static boolean getBoolean(String key, JsonObject json, String errorMessagePrefix) {
        boolean value;
        try {
            value = json.getBoolean(key);
        } catch (ClassCastException ex) {
            throw new IllegalArgumentException(errorMessagePrefix + " " + key + " is not a number");
        } catch (NullPointerException ex) {
            throw new IllegalArgumentException(errorMessagePrefix + " does not have a property '" + key + "'");
        }
        return value;
    }
}
