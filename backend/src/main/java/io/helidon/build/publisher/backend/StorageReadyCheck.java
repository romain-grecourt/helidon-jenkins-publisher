package io.helidon.build.publisher.backend;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

/**
 * Health check implementation that writes a file a reads it back to verify the storage is working properly.
 */
final class StorageReadyCheck implements HealthCheck {

    private static final Logger LOGGER = Logger.getLogger(StorageReadyCheck.class.getName());
    final Path storagePath;

    StorageReadyCheck(Path storagePath) {
        this.storagePath = storagePath;
    }

    @Override
    public HealthCheckResponse call() {
        try {
            Path path = Files.createTempFile(storagePath, "ready-check-backend", null);
            long timestamp = System.currentTimeMillis();
            try ( PrintWriter writer = new PrintWriter(Files.newOutputStream(path))) {
                writer.println(timestamp);
                writer.flush();
            }
            String line;
            try ( BufferedReader reader = new BufferedReader(Files.newBufferedReader(path))) {
                line = reader.readLine();
            }
            Files.delete(path);
            if (line != null && timestamp == Long.parseLong(line)) {
                LOGGER.log(Level.INFO, "Storage ready");
                return HealthCheckResponse.up("storage");
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
        LOGGER.log(Level.WARNING, "Storage not ready");
        return HealthCheckResponse.down("storage");
    }
}
