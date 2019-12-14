package io.helidon.build.publisher.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * File based descriptor manager.
 */
public final class PipelineDescriptorFileManager implements PipelineDescriptorManager {

    private static final Logger LOGGER = Logger.getLogger(PipelineDescriptorFileManager.class.getName());
    private static final String PIPELINE_FNAME = "pipeline.json";

    private final Path storage;
    private final ObjectMapper mapper;

    /**
     * Create a new descriptor file manager.
     * @param storage storage path
     */
    public PipelineDescriptorFileManager(Path storage) {
        this.storage = storage;
        this.mapper = new ObjectMapper();
    }

    /**
     * Load a pipeline info for a given pipeline directory.
     * @param dirPath dir path containing the pipeline descriptor
     * @return PipelineInfo or {@code null} if not found
     */
    public PipelineInfo loadInfoFromDir(Path dirPath) {
        Objects.requireNonNull(dirPath, "dirPath is null");
        Path filePath = dirPath.resolve(PIPELINE_FNAME);
        if (Files.exists(filePath)) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Reading pipeline info descriptor: {0}", filePath);
            }
            try {
                return mapper.readValue(Files.newInputStream(dirPath), PipelineInfo.class);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return null;
    }

    /**
     * Load a pipeline.
     * @param filePath pipeline descriptor file path
     * @return Pipeline
     */
    public Pipeline load(Path filePath) {
        Objects.requireNonNull(filePath, "filePath is null");
        if (Files.exists(filePath)) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Reading pipeline descriptor: {0}", filePath);
            }
            try {
                return mapper.readValue(Files.newInputStream(filePath), Pipeline.class);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return null;
    }

    @Override
    public Pipeline load(String id) {
        if (storage == null) {
            throw new IllegalStateException("storage not set");
        }
        Objects.requireNonNull(id, "id is null");
        return load(storage.resolve(id).resolve(PIPELINE_FNAME));
    }

    /**
     * Save a pipeline.
     * @param pipeline pipeline to save
     * @param filePath file path
     */
    public void save(Pipeline pipeline, Path filePath) {
        Objects.requireNonNull(pipeline, "pipeline is null");
        Objects.requireNonNull(filePath, "filePath is null");
        Path dirPath = filePath.getParent();
        try {
            if (!Files.exists(filePath) && !Files.exists(dirPath)) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Creating pipeline directory: {0}", dirPath);
                }
                Files.createDirectories(dirPath);
            }
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Writing pipeline descriptor: {0}", filePath);
            }
            mapper.writeValue(Files.newOutputStream(filePath), pipeline);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void save(Pipeline pipeline) {
        if (storage == null) {
            throw new IllegalStateException("storage not set");
        }
        Objects.requireNonNull(pipeline, "pipeline is null");
        save(pipeline, storage.resolve(pipeline.pipelineId()).resolve(PIPELINE_FNAME));
    }
}
