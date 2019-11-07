package io.helidon.build.publisher.webapp;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import io.helidon.config.Config;
import io.helidon.health.HealthSupport;
import io.helidon.health.checks.HealthChecks;
import io.helidon.media.jsonp.server.JsonSupport;
import io.helidon.metrics.MetricsSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.StaticContentSupport;
import io.helidon.webserver.WebServer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class.
 */
public final class Main {

    static final HazelcastInstance HZ = Hazelcast.newHazelcastInstance();
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    /**
     * Cannot be instantiated.
     */
    private Main() { }

    /**
     * Application main entry point.
     * @param args command line arguments.
     * @throws IOException if there are problems reading logging properties
     */
    public static void main(final String[] args) throws IOException {
        startServer();
    }

    /**
     * Start the server.
     * @return the created {@link WebServer} instance
     * @throws IOException if there are problems reading logging properties
     */
    static WebServer startServer() throws IOException {

        setupLogging();
        Config config = Config.create();
        ServerConfiguration serverConfig = ServerConfiguration.create(config.get("server"));
        WebServer server = WebServer.create(serverConfig, createRouting(config));

        server.start()
            .thenAccept(ws -> {
                System.out.println( "WEB server is up! http://localhost:" + ws.port() + "/greet");
                ws.whenShutdown().thenRun(()
                    -> System.out.println("WEB server is DOWN. Good bye!"));
                })
            .exceptionally(ex -> {
                LOGGER.log(Level.SEVERE, "Startup failed", ex);
                return null;
            });
        return server;
    }

    /**
     * Creates new {@link Routing}.
     *
     * @return routing configured with JSON support, a health check, and a service
     * @param config configuration of this server
     */
    private static Routing createRouting(Config config) {
        HazelcastInstance hz = Hazelcast.newHazelcastInstance();
        MessageBus messageBus = new MessageBus(hz);
        Config storageConfig = config.get("storage");
        Storage storage = new Storage(hz,
                storageConfig.get("cacheLocation").asString().get(),
                storageConfig.get("nThread").asInt().get(),
                storageConfig.get("ociConfigPath").asString().get(),
                storageConfig.get("ociConfigProfile").asString().get(),
                storageConfig.get("ociRegion").asString().get(),
                storageConfig.get("bucketName").asString().get());
        return Routing.builder()
                .register(JsonSupport.create())
                .register(HealthSupport.builder().addLiveness(HealthChecks.healthChecks()))
                .register(MetricsSupport.create())
                .register("/backend", new BackendService(messageBus, storage))
                .register("/api", new FrontendService(messageBus, storage))
                .register(StaticContentSupport.builder("/webapp").welcomeFileName("index.html"))
                .build();
    }

    /**
     * Configure logging from logging.properties file.
     */
    private static void setupLogging() throws IOException {
        try (InputStream is = Main.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        }
    }
}
