#!/bin/sh

DIR=$(cd $(dirname "$0") && pwd)

VM_NAME='visallo-dev'

while [ $# -gt 1 ]
do
  key="$1"

  case ${key} in
    --windows)
      USE_VM='true'
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

if [ $(uname) = 'Darwin' -o "${USE_VM}" = 'true' ]; then
  SPLIT_PERSISTENT_DIR='true'

  which docker-machine > /dev/null
  if [ $? -eq 0 ]; then
    VM_SSH="docker-machine ssh ${VM_NAME}"
  else
    VM_SSH=
  fi
fi

if [ $(uname) = 'Darwin' -o "${SPLIT_PERSISTENT_DIR}" = 'true' ]; then
  dev=$(${VM_SSH} "blkid -L boot2docker-data")
  mnt=$(echo "$(${VM_SSH} mount)" | awk -v dev=${dev} '$1 == dev && !seen {print $3; seen = 1}')
  uid=$(${VM_SSH} "id -u")
  gid=$(${VM_SSH} "id -g")
  PERSISTENT_DIR=${mnt}/visallo-dev-persistent
  ${VM_SSH} "sudo rm -rf ${PERSISTENT_DIR}"
  LOCAL_PERSISTENT_DIR=${DIR}/visallo-dev-persistent
  rm -rf ${LOCAL_PERSISTENT_DIR}
else
  PERSISTENT_DIR=${DIR}/visallo-dev-persistent
  rm -rf ${PERSISTENT_DIR}
fi
