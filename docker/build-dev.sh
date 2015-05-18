#!/bin/sh

DIR=$(cd $(dirname "$0") && pwd)
cd ${DIR}

case $(uname) in
  Linux)
    (cd ${DIR} && sudo docker build -t visallo/dev dev)
    ;;
  Darwin)
    (cd ${DIR} && docker build -t visallo/dev dev)
    ;;
  *)
    echo "unexpected uname: $(uname)"
    exit -1
    ;;
esac
