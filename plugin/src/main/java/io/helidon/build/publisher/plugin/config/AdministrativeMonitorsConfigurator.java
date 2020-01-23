package io.helidon.build.publisher.plugin.config;

import java.util.HashSet;
import java.util.Set;
import java.io.IOException;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.Attribute.Getter;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.Mapping;
import jenkins.management.AdministrativeMonitorsConfiguration;
import jenkins.model.GlobalConfiguration;

import static io.jenkins.plugins.casc.Attribute.noop;

/**
 * Configuration as code configurator to configure {@link AdministrativeMonitor}.
 * See https://github.com/jenkinsci/configuration-as-code-plugin/issues/774
 */
@Extension
public class AdministrativeMonitorsConfigurator extends BaseConfigurator<AdministrativeMonitorsConfiguration> {

    @Override
    public Class<AdministrativeMonitorsConfiguration> getTarget() {
        return AdministrativeMonitorsConfiguration.class;
    }

    @NonNull
    @Override
    public Set<Attribute<AdministrativeMonitorsConfiguration, ?>> describe() {
        HashSet<Attribute<AdministrativeMonitorsConfiguration, ?>> attributes = new HashSet<>();
        for (AdministrativeMonitor monitor : AdministrativeMonitor.all()) {
            attributes.add(new Attribute<AdministrativeMonitorsConfiguration, String>(monitor.id, Boolean.class)
            .getter(new MonitorGetter(monitor))
            .setter(noop()));
        }
        return attributes;
    }

    private static final class MonitorGetter implements Getter<AdministrativeMonitorsConfiguration, String> {

        private final AdministrativeMonitor monitor;

        MonitorGetter(AdministrativeMonitor monitor) {
            this.monitor = monitor;
        }

        @Override
        public String getValue(AdministrativeMonitorsConfiguration cfg) throws Exception {
            return String.valueOf(monitor.isEnabled());
        }
    }

    @Override
    protected AdministrativeMonitorsConfiguration instance(Mapping mapping, ConfigurationContext context) throws ConfiguratorException {
        Set<String> keys = mapping.keySet();
        try {
            for (AdministrativeMonitor monitor : AdministrativeMonitor.all()) {
                if (keys.contains(monitor.id)) {
                    if (mapping.getScalarValue(monitor.id).equalsIgnoreCase("false")) {
                        monitor.disable(true);
                    }
                }
            }
        } catch (IOException ex) {
            throw new ConfiguratorException(ex);
        }
        return GlobalConfiguration.all().get(AdministrativeMonitorsConfiguration.class);
    }
}