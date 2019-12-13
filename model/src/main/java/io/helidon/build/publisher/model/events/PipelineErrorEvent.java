package io.helidon.build.publisher.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;

/**
 * {@link PipelineEventType#PIPELINE_ERROR} event.
 */
@JsonPropertyOrder({"runId", "eventType", "code", "message"})
public final class PipelineErrorEvent extends PipelineEvent {

    final int code;
    final String message;

    /**
     * Create a new {@link PipelineEventType#ERROR} event.
     *
     * @param runId runId
     * @param code the error code
     * @param message the error message
     */
    public PipelineErrorEvent(String runId, @JsonProperty("code") int code, @JsonProperty("message") String message) {
        super(runId);
        this.code = code;
        this.message = message;
    }

    @Override
    public PipelineEventType eventType() {
        return PipelineEventType.PIPELINE_ERROR;
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
        final PipelineErrorEvent other = (PipelineErrorEvent) obj;
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
