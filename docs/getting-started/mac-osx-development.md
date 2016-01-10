# Mac OSX Developer Setup

The `$PROJECT_HOME/bin/setup-osx-env.sh` script automates installation of most of the development dependencies. The script will prompt you with additional manual steps you'll need to perform. Run it with

        bin/setup-osx-env.sh

## /etc/hosts Configuration

If you plan to do any development outside of the development docker container, which is recommended, processes on your Mac will need to talk to servers running within the docker container. By default, the code, and these instructions, refer to the VM hosting the docker container by the hostname `visallo-dev`. Therefore, the easiest way to get everything communicating correctly is to add the IP address of the VM hosting the development docker container to your Mac's `/etc/hosts` file.

1. Get the VM's IP address:

        docker-machine ip visallo-dev

1. Edit `/etc/hosts` as an administrator, adding a new line with the VM IP address:

        [docker machine ip] visallo-dev

