#!/bin/bash -eu

DIR=$(cd $(dirname "$0") && pwd)

function _usage {
  local error_message=${*:-}
  if [ "${error_message}" ]; then
    echo >&2
    echo >&2 $'\e[01;31mERROR:\e[00;00m' ${error_message}
  fi
  echo >&2
  echo >&2 '----------------------------------------------------------'
  echo >&2 $'Mac OS X \e[01;31mVisallo\e[00;00m Development Environment Setup Instructions'
  echo >&2 '----------------------------------------------------------'
  echo >&2
  echo >&2 '1) Download and install the latest Java 8 SE Development Kit from http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html'
  echo >&2 '2) Download and install Docker according to the instructions found at https://docs.docker.com/installation/mac/'
  echo >&2 '     DO NOT run the Docker installer post installation instructions!'
  echo >&2 '3) Run this script'
  echo >&2
  exit 1
}

function _message {
  local message="$*"
  echo $'\e[01;31m>>>\e[00;00m' ${message}
}

type javac &>/dev/null || _usage 'Java compiler (javac) not found in your PATH'
type docker-machine &>/dev/null || _usage 'docker-machine not found in your PATH'

_message 'Creating Docker virtial machine'
docker-machine create \
  --driver virtualbox \
  --virtualbox-memory 8192 \
  visallo-dev

eval "$(docker-machine env visallo-dev)"

_message 'Configuring Docker virtual machine'
vm_ip=$(docker-machine ssh visallo-dev ip addr show eth1 | awk '/inet / {print $2}' | awk -F / '{print $1}')
docker-machine ssh visallo-dev "sudo sed -i -e 's/visallo-dev localhost/localhost/' -e '2i ${vm_ip} visallo-dev' /etc/hosts"

_message 'Pulling the latest Visallo development Docker image'
docker pull visallo/dev

_message 'You can now start your Visallo development environment Docker container by running the following commands:'
_message '1) eval $(docker-machine env visallo-dev)'
_message "2) $(cd ${DIR}/../docker; pwd)/run-dev.sh"
