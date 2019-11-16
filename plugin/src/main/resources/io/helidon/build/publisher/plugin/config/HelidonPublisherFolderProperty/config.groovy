package io.helidon.build.publisher.plugin.config.HelidonPublisherFolderProperty

import static io.helidon.build.publisher.plugin.config.HelidonPublisherFolderProperty.DescriptorImpl.FOLDER_BLOCK_NAME

def f = namespace(lib.FormTagLib);

f.optionalBlock(name: FOLDER_BLOCK_NAME, title: _("Publish job externally"), checked: instance != null) {
    f.entry(title: _("Publisher Server"), field: "serverUrl") {
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
