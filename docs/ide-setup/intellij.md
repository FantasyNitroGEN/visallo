
# Prereqs

* Setup [configuration](configuration.md) on your host machine.
* Install [dependencies](../dependencies.md) on your host machine.
* Install the [root module](build.md#root-module) on your host machine.

# IntelliJ

## Development Jetty Web Server

* Module: `visallo-dev-jetty-server`
* Main class: `JettyWebServer`
* Program arguments:

        --webAppDir $MODULE_DIR$/../../web/war/src/main/webapp
        --port 8888
        --httpsPort 8889
        --keyStorePath $MODULE_DIR$/../../web/web-base/src/test/resources/valid.jks
        --keyStorePassword password

* Working directory: `$MODULE_DIR$/../..`

![Jetty Web Server](ide-jetty-webserver.jpg)

## Maven Profile Selection

In the Maven Projects window of IntelliJ (right side) you'll find a dropdown titled `Profiles`. The base set of profiles that should be checked are listed below.
* acl-ontology
* build-doclint-none
* build-webapp
* coordination-zookeeper
* gpw-core
* gpw-video (if you've installed ffmpeg dependency)
* queue-rabbitmq
* search-elasticsearch
* serializer-kryo
* storage-accumulo
* storage-hadoop
* web-admin
* web-auth-username-only
