package io.helidon.jenkins.publisher.config.HelidonPublisherFolderProperty

import static io.helidon.jenkins.publisher.config.HelidonPublisherFolderProperty.DescriptorImpl.FOLDER_BLOCK_NAME

def f = namespace(lib.FormTagLib);

f.optionalBlock(name: FOLDER_BLOCK_NAME, title: _("Publish job externally"), checked: instance != null) {
    f.entry(title: _("Publisher Server"), field: "serverUrl") {
        f.select()
    }
    f.entry(title: _("Excluded branches"), field: "branchExcludes") {
        f.select()
    }
}
