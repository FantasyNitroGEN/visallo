# Development Docker Image

The development docker image is the fastest and easiest way to get a development environment up and running. In fact, it's what the core Visallo development team primarily uses for day to day development.

## Creating and Starting the Container

The dev docker image contains all the backend services needed for development. To get started run:

        docker/build-dev.sh

This will build an image by downloading and installing all the necessary components into the docker image. After this completes run:

        docker/run-dev.sh

This will start the docker image and leave you in a bash shell within the image. This will also map all the internal ports to external ports so that you can run the web server against the services.

There are helper scripts within the image `/opt/start.sh` and `/opt/stop.sh` to start and stop all the services.

It is also helpful to add the following to your `/etc/hosts` file:

        127.0.0.1       visallo-dev

## Formatting

To format your dev image and start over from scratch, you can run the format script. Please be aware that this will delete all your Visallo data from the datastore.

        ./format-dev.sh

## Running

Run the commands below to start the Visallo web application. These steps must be run from the development Docker container shell resulting from running the `docker/run-dev.sh` script.

        cd /opt/visallo-source/web/war
        mvn -P queue-rabbitmq,search-elasticsearch,storage-accumulo,web-admin,web-auth-username-only jetty:run

The preceding `mvn` command will start the Visallo web application with a minimum number of features running. The `-P` option to the Maven command above specifies which profiles are included when starting Jetty. A profile groups a set of dependencies that make up a feature. Running the following command will list all of the available profiles that can be run.

        mvn help:all-profiles
