package io.helidon.jenkins.publisher;

import hudson.Extension;
import java.util.Collections;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import jenkins.scm.impl.trait.Discovery;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSourceContext;
import org.kohsuke.stapler.DataBoundConstructor;

public class SCMSourceTraitImpl extends SCMSourceTrait {

    @DataBoundConstructor
    public SCMSourceTraitImpl() {
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        GitHubSCMSourceContext ctx = (GitHubSCMSourceContext) context;
        ctx.withNotificationStrategies(Collections.singletonList(new GitHubNotificationStrategyImpl()));
    }

    @Extension
    @Discovery
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        @Override
        public String getDisplayName() {
            return "Helidon Jenkins Publisher";
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
