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

    public PipelineDescriptorFileManager(Path storage) {
        this.storage = storage;
        this.mapper = new ObjectMapper();
    }

    @Override
    public Pipeline load(String id) {
        Path filePath = storage.resolve(id).resolve(PIPELINE_FNAME);
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
        LOGGER.log(Level.WARNING, "Pipeline descriptor not found: {0}", filePath);
        return null;
    }

    @Override
    public void save(Pipeline pipeline) {
        Objects.requireNonNull(pipeline, "pipeline is null");
        Path dirPath = storage.resolve(pipeline.pipelineId());
        Path filePath = dirPath.resolve(PIPELINE_FNAME);
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
}
