#!/bin/sh

DIR=$(cd $(dirname "$0") && pwd)
cd ${DIR}

if [ $(uname) = 'Linux' ]; then
  docker ps >/dev/null 2>&1
  if [ $? -ne 0 ]; then
    SUDO=sudo
  else
    SUDO=
  fi
else
  SUDO=
fi

case $(uname) in
  Linux)
    (cd ${DIR} && ${SUDO} docker build -t visallo/dev dev)
    ;;
  Darwin)
    (cd ${DIR} && docker build -t visallo/dev dev)
    ;;
  *)
    echo "unexpected uname: $(uname)"
    exit -1
    ;;
esac
