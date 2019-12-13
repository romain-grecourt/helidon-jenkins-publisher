package io.helidon.build.publisher.model.events;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * {@link PipelineEventType#PIPELINE_ERROR} event.
 */
@JsonPropertyOrder({"pipelineId", "eventType", "code", "message"})
public final class PipelineErrorEvent extends PipelineEvent {

    final int code;
    final String message;

    /**
     * Create a new {@link PipelineEventType#ERROR} event.
     *
     * @param pipelineId pipelineId
     * @param code the error code
     * @param message the error message
     */
    public PipelineErrorEvent(String pipelineId, @JsonProperty("code") int code, @JsonProperty("message") String message) {
        super(pipelineId);
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
        hash = 89 * hash + Objects.hashCode(this.pipelineId);
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
        if (!Objects.equals(this.pipelineId, other.pipelineId)) {
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
