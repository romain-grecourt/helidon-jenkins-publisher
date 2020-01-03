package io.helidon.build.publisher.backend;

import io.helidon.common.configurable.Resource;
import io.helidon.common.pki.KeyConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;

import io.helidon.config.Config;
import io.helidon.health.HealthSupport;
import io.helidon.health.checks.HealthChecks;
import io.helidon.media.jackson.server.JacksonSupport;
import io.helidon.metrics.MetricsSupport;
import io.helidon.security.Security;
import io.helidon.security.integration.webserver.WebSecurity;
import io.helidon.security.providers.httpsign.HttpSignHeader;
import io.helidon.security.providers.httpsign.HttpSignProvider;
import io.helidon.security.providers.httpsign.InboundClientDefinition;
import io.helidon.security.providers.httpsign.SignedHeadersConfig;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.WebServer;

import static io.helidon.common.CollectionsHelper.listOf;

/**
 * Main class.
 */
public final class Main {

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
        Path storagePath = FileSystems.getDefault().getPath(config.get("storage.path").asString().get());
        WebServer server = WebServer.builder(createRouting(config, storagePath))
                .config(ServerConfiguration.create(config.get("server")))
                .addNamedRouting("admin", createAdminRouting(storagePath))
                .build();
        server.start()
            .thenAccept(ws -> {
                System.out.println( "WEB server is up! http://localhost:" + ws.port());
                ws.whenShutdown().thenRun(()
                    -> System.out.println("WEB server is DOWN. Good bye!"));
                })
            .exceptionally(ex -> {
                LOGGER.log(Level.SEVERE, "Startup failed", ex);
                return null;
            });
        return server;
    }

    private static Routing createAdminRouting(Path storagePath) {
        return Routing.builder()
                .register(HealthSupport.builder()
                        .webContext("/live")
                        .addLiveness(HealthChecks.healthChecks()))
                .register(HealthSupport.builder()
                        .webContext("/ready")
                        .addReadiness(new StorageReadyCheck(storagePath))
                        .build())
                .register(MetricsSupport.create())
                .build();
    }

    private static Routing createRouting(Config config, Path storagePath) {
        Routing.Builder routingBuilder = Routing.builder();
        WebSecurity webSecurity = createWebSecurity(config);
        if (webSecurity != null) {
            routingBuilder.register(createWebSecurity(config));
        }
        return routingBuilder
                .any(WebSecurity.secure())
                .register(JacksonSupport.create())
                .register(new BackendService(storagePath, config.get("appenderThreads").asInt().orElse(2)))
                .build();
    }

    private static WebSecurity createWebSecurity(Config config) {
        Path publicKeyPath = config.get("http-signature.public-key").asString().map(Paths::get).orElse(null);
        if (publicKeyPath == null) {
            LOGGER.log(Level.WARNING, "No HTTP signature public key provided, security is disabled");
            return null;
        }
        if (!Files.exists(publicKeyPath)) {
            throw new IllegalStateException("HTTP signature public key not found: " + publicKeyPath);
        }
        Security security = Security.builder()
                .addProvider(HttpSignProvider.builder()
                        .addAcceptHeader(HttpSignHeader.SIGNATURE)
                        .inboundRequiredHeaders(SignedHeadersConfig.builder()
                                .defaultConfig(SignedHeadersConfig.HeadersConfig.create(listOf("Host")))
                                .build())
                        .addInbound(InboundClientDefinition.builder("backend-key")
                                .principalName("RSA signature")
                                .publicKeyConfig(KeyConfig.pemBuilder()
                                        .publicKey(Resource.create(publicKeyPath))
                                        .build())
                                .build())
                        .build())
                .build();
        return WebSecurity.create(security);
    }

    private static void setupLogging() throws IOException {
        try (InputStream is = Main.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        }
    }
}
