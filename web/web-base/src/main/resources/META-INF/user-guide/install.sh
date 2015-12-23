#!/bin/bash -e

set -x

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"

USER_GUIDE=$DIR
NODE=$USER_GUIDE/node
NODE_MODULES=$USER_GUIDE/node_modules

export PATH=$NODE:$NODE_MODULES/bower/bin:$NODE_MODULES/grunt-cli/bin:$PATH

npm install
$NODE_MODULES/.bin/gitbook install .


