package io.helidon.build.publisher.plugin.config;

import hudson.model.Run;
import java.util.HashSet;
import java.util.Set;
import jenkins.model.ArtifactManager;
import jenkins.model.ArtifactManagerConfiguration;
import jenkins.model.ArtifactManagerFactory;

/**
 * A delegate artifact manager factory that is registered globally.
 */
public final class DelegateArtifactManagerFactory extends ArtifactManagerFactory {

    private static DelegateArtifactManagerFactory INSTANCE;

    private DelegateArtifactManagerFactory() {
    }

    public static DelegateArtifactManagerFactory getInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        // there may be a deserialized instance in the artifact manager configuration
        for (ArtifactManagerFactory factory : ArtifactManagerConfiguration.get().getArtifactManagerFactories()) {
            if (factory instanceof DelegateArtifactManagerFactory) {
                // found a deserialized instance
                // set it as the singleton instance
                INSTANCE = (DelegateArtifactManagerFactory) factory;
                INSTANCE.delegates = new HashSet<>();
                return INSTANCE;
            }
        }
        // create the singleton.
        INSTANCE = new DelegateArtifactManagerFactory();
        ArtifactManagerConfiguration.get().getArtifactManagerFactories().add(INSTANCE);
        return INSTANCE;
    }

    /**
     * The field transient to avoid serializing the delegates, and non final as it is set to {@code null} when de-serialized.
     */
    private transient Set<ArtifactManagerFactory> delegates = new HashSet<>();

    /**
     * Register an {@link ArtifactManagerFactory} delegate.
     * @param delegate the manager to register
     */
    public synchronized void register(ArtifactManagerFactory delegate) {
        this.delegates.add(delegate);
    }

    @Override
    public ArtifactManager managerFor(Run<?, ?> run) {
        for (ArtifactManagerFactory delegate : delegates){
            ArtifactManager manager = delegate.managerFor(run);
            if (manager != null) {
                return manager;
            }
        }
        return null;
    }
}
