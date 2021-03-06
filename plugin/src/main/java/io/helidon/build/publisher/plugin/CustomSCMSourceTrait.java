package io.helidon.build.publisher.plugin;

import hudson.Extension;
import io.helidon.build.publisher.plugin.config.Messages;
import java.util.Collections;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import jenkins.scm.impl.trait.Discovery;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSourceContext;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * An SCM behavior that replaces the notification strategies with {@link CustomGitHubNotificationStrategy}.
 */
public final class CustomSCMSourceTrait extends SCMSourceTrait {

    @DataBoundConstructor
    public CustomSCMSourceTrait() {
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        GitHubSCMSourceContext ctx = (GitHubSCMSourceContext) context;
        ctx.withNotificationStrategies(Collections.singletonList(new CustomGitHubNotificationStrategy()));
    }

    @Symbol("helidonPublisher")
    @Extension
    @Discovery
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.displayName();
        }

        @Override
        public Class<? extends SCMSourceContext> getContextClass() {
            return GitHubSCMSourceContext.class;
        }

        @Override
        public Class<? extends SCMSource> getSourceClass() {
            return GitHubSCMSource.class;
        }
    }
}
