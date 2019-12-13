package io.helidon.build.publisher.model.events;

import java.io.File;
import java.util.Objects;

/**
 * {@link PipelineEventType#ARTIFACT_DATA} event.
 */
public final class ArtifactDataEvent extends PipelineEvent {

    final File file;
    final String filename;
    final int stepsId;

    /**
     * Create a new {@link PipelineEventType#ARTIFACT_DATA} event.
     *
     * @param runId runId
     * @param stepsId the corresponding stepsId
     * @param file the artifact file
     * @param filename the artifact relative filename
     */
    public ArtifactDataEvent(String runId, int stepsId, File file, String filename) {
        super(runId);
        this.stepsId = stepsId;
        this.file = file;
        this.filename = filename;
    }

    /**
     * Get the steps id.
     *
     * @return String
     */
    public int stepsId() {
        return stepsId;
    }

    /**
     * Get the file.
     *
     * @return File
     */
    public File file() {
        return file;
    }

    /**
     * Get the relative filename.
     *
     * @return
     */
    public String filename() {
        return filename;
    }

    @Override
    public PipelineEventType eventType() {
        return PipelineEventType.ARTIFACT_DATA;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.file);
        hash = 97 * hash + Objects.hashCode(this.filename);
        hash = 97 * hash + this.stepsId;
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
        final ArtifactDataEvent other = (ArtifactDataEvent) obj;
        if (this.stepsId != other.stepsId) {
            return false;
        }
        if (!Objects.equals(this.filename, other.filename)) {
            return false;
        }
        return Objects.equals(this.file, other.file);
    }

    @Override
    public String toString() {
        return ArtifactDataEvent.class.getSimpleName() + "{"
                + " runId=" + runId
                + ", stepsId=" + stepsId
                + ", file=" + file
                + ", filename=" + filename
                + " }";
    }
}
