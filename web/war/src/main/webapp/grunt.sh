#!/bin/bash -e

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"

WEBAPP=$DIR
NODE=$WEBAPP/node
NODE_MODULES=$WEBAPP/node_modules

export PATH=$NODE:$NODE_MODULES/grunt-cli/bin:$PATH
cd $WEBAPP >/dev/null

grunt "$@"
grunt_exit_code=$?

cd - >/dev/null

exit $grunt_exit_code
