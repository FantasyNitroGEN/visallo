# Development Docker Image

The development docker image is the fastest and easiest way to get a development environment up and running. In fact, it's what the core Visallo development team primarily uses for day to day development.

## Creating and Starting the Container

The [platform specific installation instructions]() and referenced scripts should have already created the development Docker container. Run the following command to start the docker image and leave you in a bash shell within the image. This will also map all the internal ports to external ports so that you can access the web interface for Visallo and all the dependent components.

        docker/run-dev.sh

There are helper scripts within the dev Docker container (`/opt/start.sh` and `/opt/stop.sh`) to start and stop all the services.

## Source Directory Mapping

The Visallo source code you cloned is exposed to the dev Docker container at `/opt/visallo-source`. Any file changes are simultaneously available between the cloned source directory on your host OS and the `/opt/visallo-source` directory in the developer Docker container. We encourage you to do as much as possible within the dev Docker container shell.

## Formatting

To format (or re-format) your dev image, you can run the format script. Please note that this will delete all of your Visallo data.

        docker/format-dev.sh
