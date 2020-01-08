package io.helidon.build.publisher.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File based descriptor manager.
 */
public final class DescriptorFileManager implements DescriptorManager {

    private static final Logger LOGGER = Logger.getLogger(DescriptorFileManager.class.getName());
    private static final String PIPELINE_FNAME = "pipeline.json";

    private final Path storage;

    /**
     * Create a new descriptor file manager.
     * @param storage storage path
     */
    public DescriptorFileManager(Path storage) {
        this.storage = storage;
    }

    /**
     * Load a test suite result.
     * @param filePath
     * @return TestSuiteResult or {@code null} if not found
     * @throws NullPointerException if filePath is {@code null}
     */
    public TestSuiteResult loadTestSuiteResult(Path filePath) {
        Objects.requireNonNull(filePath, "filePath is null");
        if (Files.exists(filePath)) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Reading test suite result descriptor: {0}", filePath);
            }
            try {
                return JacksonSupport.read(Files.newInputStream(filePath), TestSuiteResult.class);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        LOGGER.log(Level.WARNING, "Test result descriptor does not exist: {0}", filePath);
        return null;
    }

    /**
     * Load a pipeline info for a given pipeline directory.
     * @param dirPath dir path containing the pipeline descriptor
     * @return PipelineInfo or {@code null} if not found
     * @throws NullPointerException if dirPath is {@code null}
     */
    public PipelineInfo loadInfoFromDir(Path dirPath) {
        Objects.requireNonNull(dirPath, "dirPath is null");
        Path filePath = dirPath.resolve(PIPELINE_FNAME);
        if (Files.exists(filePath)) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Reading pipeline info descriptor: {0}", filePath);
            }
            try {
                return JacksonSupport.read(Files.newInputStream(filePath), PipelineInfo.class);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        LOGGER.log(Level.WARNING, "Pipeline descriptor does not exist: {0}", filePath);
        return null;
    }

    /**
     * Load a pipeline.
     * @param filePath pipeline descriptor file path
     * @return Pipeline
     * @throws NullPointerException if filePath is {@code null}
     */
    public Pipeline loadPipeline(Path filePath) {
        Objects.requireNonNull(filePath, "filePath is null");
        if (Files.exists(filePath)) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Reading pipeline descriptor: {0}", filePath);
            }
            try {
                return JacksonSupport.read(Files.newInputStream(filePath), Pipeline.class);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        LOGGER.log(Level.WARNING, "Pipeline descriptor does not exist: {0}", filePath);
        return null;
    }

    @Override
    public Pipeline loadPipeline(String id) {
        if (storage == null) {
            throw new IllegalStateException("storage not set");
        }
        Objects.requireNonNull(id, "id is null");
        return loadPipeline(storage.resolve(id).resolve(PIPELINE_FNAME));
    }

    /**
     * Save a pipeline.
     * @param pipeline pipeline to save
     * @param filePath file path
     */
    public void savePipeline(Pipeline pipeline, Path filePath) {
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
            JacksonSupport.write(Files.newOutputStream(filePath), pipeline);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void savePipeline(Pipeline pipeline) {
        if (storage == null) {
            throw new IllegalStateException("storage not set");
        }
        Objects.requireNonNull(pipeline, "pipeline is null");
        savePipeline(pipeline, storage.resolve(pipeline.pipelineId()).resolve(PIPELINE_FNAME));
    }
}
