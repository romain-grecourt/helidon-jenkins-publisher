package io.helidon.build.publisher.plugin.config.HelidonPublisherProjectProperty

import static io.helidon.build.publisher.plugin.config.HelidonPublisherProjectProperty.DescriptorImpl.PROJECT_BLOCK_NAME

def f = namespace(lib.FormTagLib);

f.optionalBlock(name: PROJECT_BLOCK_NAME, title: _("Publish job externally"), checked: instance != null) {
    f.entry(title: _("Publisher server"), field: "serverUrl") {
        f.select()
    }
    f.entry(title: _("Excluded branches"), field: "branchExcludes") {
        f.textbox()
    }
    f.entry(title: _("Exclude synthetic steps"), field: "excludeSyntheticSteps") {
        f.checkbox(default: "true");
    }
    f.entry(title: _("Exclude meta steps"), field: "excludeMetaSteps") {
        f.checkbox(default: "true");
    }
}
