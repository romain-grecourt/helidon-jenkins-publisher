# Helidon Build Publisher

This project provides a way to publish Jenkins builds to a read-only web application.

It is composed of the following pieces:

 - plugin: a Jenkins plugin
 - model: a model abstraction for pipelines and events
 - backend: a REST application that receives events from Jenkins and
 stores metadata and files
 - frontend-api: a REST application that provides an API for the frontend ui
 - frontend-ui: a single page application to display the builds

## Jenkins plugin

See [plugin/README.md](./plugin/README.md)

## Deployment

See [k8s/README.md](./k8s/README.md)