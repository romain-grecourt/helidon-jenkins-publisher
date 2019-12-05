package io.helidon.build.publisher.plugin.config.HelidonPublisherServer

def f = namespace(lib.FormTagLib);
def c = namespace(lib.CredentialsTagLib)

f.entry(title: _("Server URL"), field: "serverUrl") {
    f.textbox()
}

f.entry(title: _("Credentials"), field: "credentialsId") {
    c.select(context:app, includeUser:false, expressionAllowed:false)
}

f.block() {
    f.validateButton(
            title: _("Test connection"),
            progress: _("Testing..."),
            method: "verifyCredentials",
            with: "serverUrl,credentialsId"
    )
}

f.entry(title: _("Client threads"), field: "nthreads") {
    f.number(default: "5")
}