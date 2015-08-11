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
    echo "2) Download and install boot2docker according to the instructions found at https://docs.docker.com/installation/mac/ (do not run boot2docker found in the /Applications folder following installation)."
    echo "3) Run this script."
    echo ""
    exit 1
}

type javac >/dev/null 2>&1 || { echo >&2 "javac compiler for Java was not found. Please see the installation instructions below for more information."; usage; }
type boot2docker >/dev/null 2>&1 || { echo >&2 "boot2docker was not found. If you've already installed boot2docker, make sure it is in your PATH. Please see the installation instructions below for more information."; usage; }

echo ">> Initializing boot2docker VM. This may take a while."
boot2docker init -m 8192

echo ">> Starting boot2docker VM."
boot2docker up

echo ">> Configuring boot2docker VM."
eval "$(boot2docker shellinit)"
vm_ip=$(boot2docker ssh ip addr show eth1 | awk '/inet / {print $2}' | awk -F / '{print $1}')
boot2docker ssh "sudo sed -i -e 's/boot2docker localhost/localhost/' -e '2i ${vm_ip} visallo-dev' /etc/hosts"

RUN_SCRIPT_PATH=$(cd ../docker; pwd)/run-dev.sh

echo ""
echo ">> You can now start your development environment container by running the following commands."
echo '>> 1) eval "$(boot2docker shellinit)"'
echo ">> 2) $RUN_SCRIPT_PATH"

