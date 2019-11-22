package io.helidon.build.publisher.model;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Pipeline events.
 */
public final class PipelineEvents {

    final List<Event> events;

    /**
     * Create a new pipeline events.
     * @param events events
     */
    public PipelineEvents(@JsonProperty("events") List<Event> events) {
        this.events = new LinkedList<>(events);
    }

    /**
     * Get the events.
     * @return {@code List<AbstractEvent>}
     */
    @JsonProperty
    public List<Event> events() {
        return events;
    }

    /**
     * Event type.
     */
    public enum EventType {

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
         * Output is available.
         */
        OUTPUT,

        /**
         * Output data has been produced.
         */
        OUTPUT_DATA,

        /**
         * A pipeline has been created.
         */
        PIPELINE_CREATED,

        /**
         * A pipeline has been completed.
         */
        PIPELINE_COMPLETED,

        /**
         * An error occurred.
         */
        ERROR
    }

    /**
     * Abstract event.
     */
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "eventType")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = PipelineEvents.PipelineCreated.class, name = "PIPELINE_CREATED"),
        @JsonSubTypes.Type(value = PipelineEvents.PipelineCompleted.class, name = "PIPELINE_COMPLETED"),
        @JsonSubTypes.Type(value = PipelineEvents.StepCreated.class, name = "STEP_CREATED"),
        @JsonSubTypes.Type(value = PipelineEvents.StepCompleted.class, name = "STEP_COMPLETED"),
        @JsonSubTypes.Type(value = PipelineEvents.StageCreated.class, name = "STAGE_CREATED"),
        @JsonSubTypes.Type(value = PipelineEvents.StageCompleted.class, name = "STAGE_COMPLETED"),
        @JsonSubTypes.Type(value = PipelineEvents.Output.class, name = "OUTPUT"),
        @JsonSubTypes.Type(value = PipelineEvents.OutputData.class, name = "OUTPUT_DATA"),
        @JsonSubTypes.Type(value = PipelineEvents.Error.class, name = "ERROR")
    })
    public static abstract class Event {

        final String runId;

        /**
         * Create a new event.
         * @param runId run id
         */
        Event(String runId) {
            this.runId = runId;
        }

        /**
         * Get the run id.
         * @return String
         */
        @JsonProperty
        public final String runId() {
            return runId;
        }

        /**
         * Get the event type.
         *
         * @return EventType, never {@code null}
         */
        @JsonProperty
        public abstract EventType eventType();
    }

    /**
     * Event listener.
     */
    public interface EventListener {

        /**
         * Process an event.
         *
         * @param event the event to process
         */
        void onEvent(Event event);
    }

    /**
     * No-op event listener implementation.
     */
    private static class NoOpEventListener implements EventListener {

        @Override
        public void onEvent(Event event) {
        }
    }

    /**
     * No-op event listener.
     */
    public static EventListener NOOP_LISTENER = new NoOpEventListener();

    /**
     * Node event type.
     */
    public enum NodeEventType {

        /**
         * A graph node has been created.
         */
        CREATED,

        /**
         * A graph node has been completed.
         */
        COMPLETED
    }

    /**
     * Abstract node created event.
     */
    public static abstract class NodeCreatedEvent extends Event {

        final int id;
        final int parentId;
        final int index;
        final String name;
        final String path;
        final long startTime;

        /**
         * Create a new node created event.
         * @param runId run id
         * @param id node id
         * @param parentId node parent id
         * @param index index in the parent node
         * @param name node name, may be {@code null}
         * @param startTime start timestamp
         */
        NodeCreatedEvent(String runId, int id, int parentId, int index, String name, String path, long startTime) {
            super(runId);
            this.id = id;
            this.parentId = parentId;
            this.index = index;
            this.name = name;
            this.path = path;
            this.startTime = startTime;
        }

        /**
         * Get the node id.
         *
         * @return int
         */
        @JsonProperty
        public final int id() {
            return id;
        }

        /**
         * Get the parent node id.
         *
         * @return int
         */
        @JsonProperty
        public final int parentId() {
            return parentId;
        }

        /**
         * Get the parent index.
         * @return int
         */
        @JsonProperty
        public final int index() {
            return index;
        }

        /**
         * Get the name of the created node.
         * @return String
         */
        @JsonProperty
        public final String name() {
            return name;
        }

        /**
         * Get the path of the created node.
         * @return String
         */
        @JsonProperty
        public String path() {
            return path;
        }

        /**
         * Get the start timestamp.
         * @return long
         */
        @JsonProperty
        public final long startTime() {
            return startTime;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 59 * hash + this.id;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final NodeCreatedEvent other = (NodeCreatedEvent) obj;
            if (!Objects.equals(this.runId, other.runId)) {
                return false;
            }
            if (this.id != other.id) {
                return false;
            }
            if (this.parentId != other.parentId) {
                return false;
            }
            if (this.index != other.index) {
                return false;
            }
            if (this.startTime != other.startTime) {
                return false;
            }
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            return Objects.equals(this.path, other.path);
        }
    }

    /**
     * Abstract node completed event.
     */
    public static abstract class NodeCompletedEvent extends Event {

        final EventType type;
        final int id;
        final Status.Result result;
        final long endTime;

        /**
         * Create a new completed node event.
         * @param runId run id
         * @param id node id
         * @param state node state
         * @param result node result
         * @param endTime node end timestamp
         */
        NodeCompletedEvent(String runId, EventType type, int id, Status.Result result, long endTime) {
            super(runId);
            this.type = type;
            this.id = id;
            this.result = result;
            this.endTime = endTime;
        }

        @JsonProperty
        @Override
        public final EventType eventType() {
            return type;
        }

        /**
         * Get the node id.
         *
         * @return int
         */
        @JsonProperty
        public final int id() {
            return id;
        }

        /**
         * Get the result.
         * @return Status.Result
         */
        @JsonProperty
        public final Status.Result result() {
            return result;
        }

        /**
         * Get the end timestamp.
         * @return long
         */
        @JsonProperty
        public final long endTime() {
            return endTime;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "{"
                    + " runId=" + runId
                    + ", id=" + id
                    + ", result=" + result
                    + ", endTime=" + endTime
                    + " }";
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + Objects.hashCode(this.type);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final NodeCompletedEvent other = (NodeCompletedEvent) obj;
            if (this.id != other.id) {
                return false;
            }
            if (this.endTime != other.endTime) {
                return false;
            }
            if (this.type != other.type) {
                return false;
            }
            return this.result == other.result;
        }
    }

    /**
     * {@link EventType#STEP_CREATED} event.
     */
    @JsonPropertyOrder({"runId", "eventType", "id", "parentId", "index", "name", "path", "startTime", "state", "args", "declared"})
    public static final class StepCreated extends NodeCreatedEvent {

        final String args;
        final boolean meta;
        final boolean declared;

        /**
         * Create a new {@link EventType#STEP_CREATED} event.
         * @param runId run id
         * @param id node id
         * @param parentId node parent id
         * @param index index in the parent node
         * @param name node name
         * @param path
         * @param startTime start timestamp
         * @param args step arguments
         * @param meta step meta flag
         * @param declared step declared flag
         */
        public StepCreated(@JsonProperty("runId") String runId, @JsonProperty("id") int id,
                @JsonProperty("parentId") int parentId, @JsonProperty("index") int index, @JsonProperty("name") String name,
                @JsonProperty("path") String path, @JsonProperty("startTime") long startTime, @JsonProperty("args") String args,
                @JsonProperty("meta") boolean meta, @JsonProperty("declared") boolean declared) {

            super(runId, id, parentId, index, name, path, startTime);
            this.args = args;
            this.meta = meta;
            this.declared = declared;
        }

        @Override
        public EventType eventType() {
            return EventType.STEP_CREATED;
        }

        /**
         * Get the step arguments.
         * @return String
         */
        @JsonProperty
        public String args() {
            return args;
        }

        /**
         * Is the step declared.
         * @return {@code boolean}
         */
        @JsonProperty
        public boolean declared() {
            return declared;
        }

        /**
         * Is the step meta.
         * @return {@code boolean}
         */
        @JsonProperty
        public boolean meta() {
            return meta;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StepCreated other = (StepCreated) obj;
            if (this.meta != other.meta) {
                return false;
            }
            if (this.declared != other.declared) {
                return false;
            }
            if(!Objects.equals(this.args, other.args)){
                return false;
            }
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            // hash code based on the node id only
            return super.hashCode();
        }

        @Override
        public String toString() {
            return StepCreated.class.getSimpleName() + "{"
                    + " runId=" + runId
                    + ", id=" + id
                    + ", parentId=" + parentId
                    + ", index=" + index
                    + ", name=" + name
                    + ", path=" + path
                    + ", args=" + args
                    + ", meta=" + meta
                    + ", declared=" + declared
                    + ", startTime=" + startTime
                    + " }";
        }
    }

    /**
     * {@link EventType#STEP_COMPLETED} event.
     */
    @JsonPropertyOrder({"runId", "eventType", "id","state", "result", "endTime"})
    public static final class StepCompleted extends NodeCompletedEvent {

        /**
         * Create a new {@link EventType#STEP_COMPLETED} event.
         * @param runId runId
         * @param id node id
         * @param result node result
         * @param endTime node end timestamp
         */
        public StepCompleted(@JsonProperty("runId") String runId, @JsonProperty("id") int id,
                @JsonProperty("result") Status.Result result, @JsonProperty("endTime") long endTime) {

            super(runId, EventType.STEP_COMPLETED, id, result, endTime);
        }
    }

    /**
     * {@link EventType#STAGE_CREATED} event.
     */
    @JsonPropertyOrder({"runId", "eventType", "id", "parentId", "index", "name", "path", "startTime", "state", "stageType"})
    public static final class StageCreated extends NodeCreatedEvent {

        final Pipeline.Stage.StageType stageType;

        /**
         * Create a new {@link EventType#STAGE_CREATED} event.
         *
         * @param id node id
         * @param runId runId
         * @param parentId node parent id
         * @param index index in the parent node
         * @param name node name
         * @param path node path
         * @param startTime start timestamp
         * @param stageType stage type
         */
        public StageCreated(@JsonProperty("runId") String runId, @JsonProperty("id") int id,
                @JsonProperty("parentId") int parentId, @JsonProperty("index") int index, @JsonProperty("name") String name,
                @JsonProperty("path") String path, @JsonProperty("startTime") long startTime,
                @JsonProperty("stageType") Pipeline.Stage.StageType stageType) {

            super(runId, id, parentId, index, name, path, startTime);
            this.stageType = stageType;
        }

        @Override
        public EventType eventType() {
            return EventType.STAGE_CREATED;
        }

        /**
         * Get the stage type.
         * @return Stage.Type
         */
        @JsonProperty
        public Pipeline.Stage.StageType stageType() {
            return stageType;
        }

        @Override
        public int hashCode() {
            // hash code based on the node id only
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StageCreated other = (StageCreated) obj;
            if (!Objects.equals(this.runId, other.runId)) {
                return false;
            }
            if (this.stageType != other.stageType) {
                return false;
            }
            return super.equals(obj);
        }

        @Override
        public String toString() {
            return StageCreated.class.getSimpleName() + "{"
                    + " runId=" + runId
                    + ", type=" + stageType
                    + ", id=" + id
                    + ", parentId=" + parentId
                    + ", index=" + index
                    + ", name=" + name
                    + ", path=" + path
                    + ", startTime=" + startTime
                    + " }";
        }
    }

    /**
     * {@link EventType#STAGE_COMPLETED} event.
     */
    @JsonPropertyOrder({"runId", "eventType", "id", "state", "result", "endTime"})
    public static final class StageCompleted extends NodeCompletedEvent {

        /**
         * Create a new {@link EventType#STAGE_COMPLETED} event.
         *
         * @param runId runId
         * @param id node id
         * @param result node result
         * @param endTime node end timestamp
         */
        public StageCompleted(@JsonProperty("runId") String runId, @JsonProperty("id") int id,
                @JsonProperty("result") Status.Result result, @JsonProperty("endTime") long endTime) {

            super(runId, EventType.STAGE_COMPLETED, id, result, endTime);
        }
    }

    /**
     * {@link EventType#OUTPUT_DATA} event.
     */
    public static final class OutputData extends Event {

        final int stepId;
        final byte[] data;

        /**
         * Create a new {@link EventType#OUTPUT_DATA} event.
         * @param runId runId
         * @param stepId the corresponding stepId
         * @param data the output data
         */
        public OutputData(String runId, int stepId, byte[] data) {
            super(runId);
            this.stepId = stepId;
            this.data = data;
        }

        /**
         * Get the step id.
         * @return String
         */
        public int stepId() {
            return stepId;
        }

        /**
         * Get the output data.
         * @return byte[]
         */
        public byte[] data() {
            return data;
        }

        @Override
        public EventType eventType() {
            return EventType.OUTPUT_DATA;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.runId);
            hash = 53 * hash + Objects.hashCode(this.stepId);
            hash = 53 * hash + Arrays.hashCode(this.data);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final OutputData other = (OutputData) obj;
            if (!Objects.equals(this.runId, other.runId)) {
                return false;
            }
            if (!Objects.equals(this.stepId, other.stepId)) {
                return false;
            }
            return Arrays.equals(this.data, other.data);
        }

        @Override
        public String toString() {
            return OutputData.class.getSimpleName() + "{"
                    + " runId=" + runId
                    + ", stepId=" + stepId
                    + " }";
        }
    }

    /**
     * {@link EventType#OUTPUT} event.
     */
    @JsonPropertyOrder({"runId", "eventType", "stepId"})
    public static final class Output extends Event {

        final int stepId;

        /**
         * Create a new {@link EventType#OUTPUT} event.
         * @param runId runId
         * @param stepId the corresponding step id
         */
        public Output(@JsonProperty("runId") String runId, @JsonProperty("stepId") int stepId) {
            super(runId);
            this.stepId = stepId;
        }

        /**
         * Get the step id.
         * @return String
         */
        @JsonProperty
        public int stepId() {
            return stepId;
        }

        @Override
        public EventType eventType() {
            return EventType.OUTPUT;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 47 * hash + Objects.hashCode(this.runId);
            hash = 47 * hash + Objects.hashCode(this.stepId);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Output other = (Output) obj;
            if (!Objects.equals(this.runId, other.runId)) {
                return false;
            }
            return Objects.equals(this.stepId, other.stepId);
        }

        @Override
        public String toString() {
            return Output.class.getSimpleName() + "{"
                    + " stepId=" + stepId
                    + " }";
        }
    }

    /**
     * {@link EventType#PIPELINE_CREATED} event.
     */
    @JsonPropertyOrder({"runId", "eventType", "jobName", "scmHead", "scmHash", "startTime"})
    public static final class PipelineCreated extends Event {

        final String jobName;
        final String scmHead;
        final String scmHash;
        final long startTime;

        /**
         * Create a new {@link EventType#PIPELINE_CREATED} event.
         * @param runId runId
         * @param jobName job name
         * @param scmHead SCM head
         * @param scmHash SCM hash
         * @param startTime start timestamp
         */
        public PipelineCreated(@JsonProperty("runId") String runId, @JsonProperty("jobName") String jobName,
                @JsonProperty("scmHead") String scmHead, @JsonProperty("scmHash") String scmHash,
                @JsonProperty("startTime") long startTime) {

            super(runId);
            this.jobName = jobName;
            this.scmHead = scmHead;
            this.scmHash = scmHash;
            this.startTime = startTime;
        }

        @Override
        public EventType eventType() {
            return EventType.PIPELINE_CREATED;
        }

        /**
         * Get the job name.
         * @return String
         */
        @JsonProperty
        public String jobName() {
            return jobName;
        }

        /**
         * Get the SCM head.
         * @return String
         */
        @JsonProperty
        public String scmHead() {
            return scmHead;
        }

        /**
         * Get the SCM hash.
         * @return String
         */
        @JsonProperty
        public String scmHash() {
            return scmHash;
        }

        /**
         * Get the start timestamp.
         * @return long
         */
        @JsonProperty
        public long startTime() {
            return startTime;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + Objects.hashCode(this.runId);
            hash = 89 * hash + Objects.hashCode(this.jobName);
            hash = 89 * hash + Objects.hashCode(this.scmHead);
            hash = 89 * hash + Objects.hashCode(this.scmHash);
            hash = 89 * hash + Objects.hashCode(this.startTime);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PipelineCreated other = (PipelineCreated) obj;
            if (!Objects.equals(this.runId, other.runId)) {
                return false;
            }
            if (!Objects.equals(this.jobName, other.jobName)) {
                return false;
            }
            if (!Objects.equals(this.scmHead, other.scmHead)) {
                return false;
            }
            if (!Objects.equals(this.scmHash, other.scmHash)) {
                return false;
            }
            return Objects.equals(this.startTime, other.startTime);
        }

        @Override
        public String toString() {
            return PipelineCreated.class.getSimpleName() + "{"
                    + " runId=" + runId
                    + ", jobName=" + jobName
                    + ", scmHead=" + scmHead
                    + ", scmHash=" + scmHash
                    + ", startTime=" + startTime
                    + " }";
        }
    }

    /**
     * {@link EventType#PIPELINE_COMPLETED} event.
     */
    @JsonPropertyOrder({"runId", "eventType", "state", "result", "endTime"})
    public static final class PipelineCompleted extends Event {

        /**
         * Create a new {@link EventType#PIPELINE_COMPLETED} event.
         * @param runId runId
         */
        public PipelineCompleted(@JsonProperty("runId") String runId) {
            super(runId);
        }

        @Override
        public EventType eventType() {
            return EventType.PIPELINE_COMPLETED;
        }

        @Override
        public int hashCode() {
            int hash = 8;
            hash = 89 * hash + Objects.hashCode(this.runId);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PipelineCompleted other = (PipelineCompleted) obj;
            return Objects.equals(this.runId, other.runId);
        }

        @Override
        public String toString() {
            return PipelineCompleted.class.getSimpleName() + "{"
                    + " runId=" + runId
                    + " }";
        }
    }

    /**
     * {@link EventType#ERROR} event.
     */
    @JsonPropertyOrder({"runId", "eventType", "code", "message"})
    public static final class Error extends Event {

        final int code;
        final String message;

        /**
         * Create a new {@link EventType#ERROR} event.
         * @param runId runId
         * @param code the error code
         * @param message the error message
         */
        public Error(String runId, @JsonProperty("code") int code, @JsonProperty("message") String message) {
            super(runId);
            this.code = code;
            this.message = message;
        }

        @Override
        public EventType eventType() {
            return EventType.ERROR;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 89 * hash + this.code;
            hash = 89 * hash + Objects.hashCode(this.runId);
            hash = 89 * hash + Objects.hashCode(this.message);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Error other = (Error) obj;
            if (this.code != other.code) {
                return false;
            }
            if (!Objects.equals(this.runId, other.runId)) {
                return false;
            }
            return Objects.equals(this.message, other.message);
        }

        @Override
        public String toString() {
            return Error.class.getSimpleName() + "{"
                    + " code=" + code
                    + ", message=" + message
                    + " }";
        }
    }
}
