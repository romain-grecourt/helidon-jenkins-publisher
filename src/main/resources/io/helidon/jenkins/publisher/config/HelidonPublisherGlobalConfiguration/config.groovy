package io.helidon.jenkins.publisher.config.HelidonPublisherGlobalConfiguration

def f = namespace(lib.FormTagLib);

f.section(title: descriptor.displayName) {
    f.entry(title: _("Servers")) {
        f.repeatableHeteroProperty(
                field: "servers",
                hasHeader: "true",
                addCaption: _("Add Server"))
    }
}
