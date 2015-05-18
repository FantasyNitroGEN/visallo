#!/bin/sh

DIR=$(cd $(dirname "$0") && pwd)

VM_NAME='boot2docker-vm'

while [[ $# > 1 ]]
do
  key="$1"

  case ${key} in
    --boot2docker)
      USE_BOOT2DOCKER='true'
      ;;
    --vm)
      VM_NAME="$2"
      shift
      ;;
    *)
      ;;
  esac
  shift
done

if [ $(uname) = 'Darwin' -o "${USE_BOOT2DOCKER}" = 'true' ]; then
  SPLIT_PERSISTENT_DIR='true'

  which boot2docker > /dev/null
  if [ $? -eq 0 ]; then
    BOOT2DOCKER_SSH="boot2docker --vm=${VM_NAME} ssh"
  else
    BOOT2DOCKER_SSH=
  fi
fi

if [ $(uname) = 'Darwin' -o "${SPLIT_PERSISTENT_DIR}" = 'true' ]; then
  dev=$(${BOOT2DOCKER_SSH} blkid -L boot2docker-data)
  mnt=$(echo "$(${BOOT2DOCKER_SSH} mount)" | awk -v dev=${dev} '$1 == dev && !seen {print $3; seen = 1}')
  uid=$(${BOOT2DOCKER_SSH} id -u)
  gid=$(${BOOT2DOCKER_SSH} id -g)
  PERSISTENT_DIR=${mnt}/visallo-dev-persistent
  ${BOOT2DOCKER_SSH} sudo rm -rf ${PERSISTENT_DIR}
  LOCAL_PERSISTENT_DIR=${DIR}/visallo-dev-persistent
  rm -rf ${LOCAL_PERSISTENT_DIR}
else
  PERSISTENT_DIR=${DIR}/visallo-dev-persistent
  rm -rf ${PERSISTENT_DIR}
fi
