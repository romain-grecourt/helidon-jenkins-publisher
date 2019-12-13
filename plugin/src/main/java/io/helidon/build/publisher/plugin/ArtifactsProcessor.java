package io.helidon.build.publisher.plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.build.publisher.model.Steps;
import io.helidon.build.publisher.model.events.ArtifactDataEvent;
import io.helidon.build.publisher.model.events.ArtifactsInfoEvent;
import io.helidon.build.publisher.plugin.config.DelegateArtifactManagerFactory;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.util.FileVisitor;
import jenkins.model.ArtifactManager;
import jenkins.model.ArtifactManagerFactory;
import jenkins.model.StandardArtifactManager;


/**
 * Delegating artifact manager that matches artifacts to steps.
 */
final class ArtifactsProcessor extends StandardArtifactManager {

    /**
     * Provider of steps to matched with artifacts being archived.
     */
    interface StepsProvider {

        /**
         * Get the steps to associate with archived artifacts.
         *
         * @return Steps or {@code null} if the artifacts are not associated with any steps.
         */
        Steps getSteps();
    }

    /**
     * ArtifactsInterceptor factory.
     */
    interface Factory {

        /**
         * Create a new artifacts interceptor for the given run.
         * @param run the run
         * @return ArtifactsInterceptor or {@code null} if the is not to be intercepted
         */
        ArtifactsProcessor create(Run<?,?> run);
    }

    private static final ArtifactManagerFactory ARTIFACT_MANAGER_FACTORY = new ArtifactManagerFactoryImpl();
    private static transient HashSet<Factory> factories = new HashSet<>();

    private final transient BackendClient client;
    private final transient String pipelineId;
    private final transient StepsProvider stepsProvider;

    protected ArtifactsProcessor(Run<?, ?> build, BackendClient client, String pipelineId, StepsProvider stepsProvider) {
        super(build);
        this.client = Objects.requireNonNull(client, "client is null!");
        this.pipelineId = Objects.requireNonNull(pipelineId, "pipelineId is null!");
        this.stepsProvider = Objects.requireNonNull(stepsProvider, "steps provider is null!");
    }

    @Override
    public final void archive(FilePath workspace, Launcher launcher, BuildListener listener, final Map<String, String> artifacts)
            throws IOException, InterruptedException {

        Steps steps = stepsProvider != null ? stepsProvider.getSteps() : null;
        super.archive(workspace, launcher, listener, artifacts);
        if (steps != null) {
            final int stepsId = steps.id();
            final AtomicInteger artifactsCount = new AtomicInteger(0);
            new FilePath.ExplicitlySpecifiedDirScanner(artifacts).scan(getArtifactsDir(), new FileVisitor() {
                @Override
                public void visit(File file, String relativePath) throws IOException {
                    client.onEvent(new ArtifactDataEvent(pipelineId, stepsId, file, relativePath));
                    artifactsCount.incrementAndGet();
                }
            });
            int count = artifactsCount.get();
            if (count > 0) {
                client.onEvent(new ArtifactsInfoEvent(pipelineId, stepsId, count));
            }
        }
    }

    /**
     * Register a factory.
     * @param factory factory to register
     */
    static void register(Factory factory) {
        if (factories == null) {
            factories = new HashSet<>();
        }
        factories.add(Objects.requireNonNull(factory, "factory is null"));
        // make sure the real factory is hooked-in
        DelegateArtifactManagerFactory.getInstance().register(ARTIFACT_MANAGER_FACTORY);
    }

    @SuppressWarnings("deprecation")
    private File getArtifactsDir() {
        return build.getArtifactsDir();
    }

    /**
     * {@link ArtifactManagerFactory} that produces {@link ArtifactsProcessor} using the registered {@link Factory}.
     */
    private static final class ArtifactManagerFactoryImpl extends ArtifactManagerFactory {

        @Override
        public ArtifactManager managerFor(Run<?, ?> run) {
            if (factories != null) {
                for (Factory factory : factories) {
                    ArtifactsProcessor interceptor = factory.create(run);
                    if (interceptor != null) {
                        return interceptor;
                    }
                }
            }
            return null;
        }
    }
}
