#!/bin/bash

DIR=$(cd $(dirname "$0") && pwd)
cd ${DIR}/target/dist/lib

for coreLib in ./core/*.jar; do
  coreLib=$(basename ${coreLib})
  find . -type d \( -path ./core -o -path ./client-api \) -prune -o -print | grep ${coreLib} | xargs rm -f
done
