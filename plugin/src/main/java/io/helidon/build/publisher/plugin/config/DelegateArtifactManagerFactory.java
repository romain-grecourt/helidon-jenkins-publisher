package io.helidon.build.publisher.plugin.config;

import java.util.HashSet;
import java.util.Set;

import hudson.model.Run;
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
        synchronized (DelegateArtifactManagerFactory.class) {
            // there may be a deserialized instance in the artifact manager configuration
            for (ArtifactManagerFactory factory : ArtifactManagerConfiguration.get().getArtifactManagerFactories()) {
                if (factory instanceof DelegateArtifactManagerFactory) {
                    // found a deserialized instance
                    // set it as the singleton instance
                    INSTANCE = (DelegateArtifactManagerFactory) factory;
                    return INSTANCE;
                }
            }
            // create the singleton.
            INSTANCE = new DelegateArtifactManagerFactory();
            ArtifactManagerConfiguration.get().getArtifactManagerFactories().add(INSTANCE);
        }
        return INSTANCE;
    }

    /**
     * The field is transient to avoid serializing the delegates, and non final as it is set to {@code null} when de-serialized.
     */
    private transient Set<ArtifactManagerFactory> delegates;

    /**
     * Register an {@link ArtifactManagerFactory} delegate.
     * @param delegate the manager to register
     */
    public void register(ArtifactManagerFactory delegate) {
        synchronized (DelegateArtifactManagerFactory.class) {
            if (delegates == null) {
                delegates = new HashSet<>();
            }
            delegates.add(delegate);
        }
    }

    @Override
    public ArtifactManager managerFor(Run<?, ?> run) {
        if (delegates != null) {
            for (ArtifactManagerFactory delegate : delegates){
                ArtifactManager manager = delegate.managerFor(run);
                if (manager != null) {
                    return manager;
                }
            }
        }
        return null;
    }
}
