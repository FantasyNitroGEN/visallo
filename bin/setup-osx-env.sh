#!/bin/bash -eu

DIR=$(cd $(dirname "$0") && pwd)

usage()
{
    echo ""
    echo "----------------------------------------------------------"
    echo "Mac OSX Visallo Development Environment Setup Instructions"
    echo "----------------------------------------------------------"
    echo ""
    echo "1) Download and install the latest Java 8 SE Development Kit from http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html"
    echo "2) Download and install docker according to the instructions found at https://docs.docker.com/installation/mac/ but do not follow any of the installer's instructions post installation."
    echo "3) Run this script."
    echo ""
    exit 1
}

type javac >/dev/null 2>&1 || { echo >&2 "javac compiler for Java was not found. Please see the installation instructions below for more information."; usage; }
type docker-machine >/dev/null 2>&1 || { echo >&2 "docker-machine was not found. If you've already installed docker-machine, make sure it is in your PATH. Please see the installation instructions below for more information."; usage; }

echo ">> Creating docker-machine VM. This may take a while."
docker-machine create \
  --driver virtualbox \
  --virtualbox-memory 8192 \
  --virtualbox-boot2docker-url https://github.com/boot2docker/boot2docker/releases/download/v1.8.1/boot2docker.iso \
  visallo-dev

eval "$(docker-machine env visallo-dev)"

echo ">> Configuring docker-machine VM"
vm_ip=$(docker-machine ssh visallo-dev ip addr show eth1 | awk '/inet / {print $2}' | awk -F / '{print $1}')
docker-machine ssh visallo-dev "sudo sed -i -e 's/visallo-dev localhost/localhost/' -e '2i ${vm_ip} visallo-dev' /etc/hosts"

$(cd ${DIR}/../docker; pwd)/build-dev.sh

echo ">> Building the development Docker container"
RUN_SCRIPT_PATH=$(cd ${DIR}/../docker; pwd)/run-dev.sh

echo ""
echo ">> You can now start your development environment container by running the following commands."
echo '>> 1) eval "$(docker-machine env visallo-dev)"'
echo ">> 2) $RUN_SCRIPT_PATH"
