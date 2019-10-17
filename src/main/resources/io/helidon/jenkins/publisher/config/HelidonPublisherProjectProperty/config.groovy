package io.helidon.jenkins.publisher.config.HelidonPublisherProjectProperty

import static io.helidon.jenkins.publisher.config.HelidonPublisherProjectProperty.DescriptorImpl.PROJECT_BLOCK_NAME

def f = namespace(lib.FormTagLib);

f.optionalBlock(name: PROJECT_BLOCK_NAME, title: _("Publish job externally"), checked: instance != null) {
    f.entry(title: _("Publisher server"), field: "serverName") {
        f.select()
    }
}
