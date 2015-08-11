# Development Docker Image

The development docker image is the fastest and easiest way to get a development environment up and running. In fact, it's what the core Visallo development team primarily uses for day to day development.

## Running

The dev docker image contains all the backend services needed for development. To get started run:

        docker/build-dev.sh

This will build an image by downloading and installing all the necessary components into the docker image. After this completes run:

        docker/run-dev.sh

This will start the docker image and leave you in a bash shell within the image. This will also map all the internal ports to external ports so that you can access the web interface for Visallo and all the dependent components.

There are helper scripts within the dev Docker container (`/opt/start.sh` and `/opt/stop.sh`) to start and stop all the services.

## Source Directory Mapping

The Visallo source code you cloned is exposed to the dev Docker container at `/opt/visallo-source`. Any file changes are simultaneously available between the cloned source directory on your host OS and the `/opt/visallo-source` directory in the developer Docker container. We encourage you to do as much as possible within the dev Docker container shell.

## Formatting

To format (or re-format) your dev image, you can run the format script. Please note that this will delete all of your Visallo data.

        docker/format-dev.sh

## Docker Web Server

1. Create a war file:<br/>
      _NOTE: Run from the host machine in the root of your clone._<br/>
      _NOTE: Requires Oracle JDK._

        mvn package -pl web/war -am -DskipTests -Dsource.skip=true

      If you get bower ESUDO error you need to create a ~/.bowerrc in the root folder and add this code

        {
          "allow-root": true
        }

1. Copy the war file:

        cp web/war/target/visallo-web-war*.war \
           docker/visallo-dev-persistent/opt/jetty/webapps/root.war

1. Package an auth plugin:

        mvn package -pl ./web/plugins/auth-username-only -am -DskipTests

1. Copy the auth plugin for use in the docker image:

        cp web/plugins/auth-username-only/target/visallo-web-auth-username-only-*[0-9T].jar \
           docker/visallo-dev-persistent/opt/visallo/lib

1. Inside the docker image run Jetty:

        /opt/jetty/bin/jetty.sh start

1. Open a browser and go to: `http://visallo-dev:8080/`
