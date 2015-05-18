#!/bin/bash -e

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"

WEBAPP=$DIR/../webapp
export PATH=$WEBAPP/node:$WEBAPP/node_modules/bower/bin:$WEBAPP/node_modules/grunt-cli/bin:$PATH
cd $WEBAPP >/dev/null

# Run `bower list` for previous `bower list` output
mkdir -p ${DIR}/../../../target
filename=${DIR}/../../../target/.webapp-build.$(id -un)
filename_previous=${DIR}/../../../target/.webapp-build.previous.$(id -un)

touch $filename
mv $filename $filename_previous
bower list --offline > $filename

if diff $filename $filename_previous >/dev/null ; then
  grunt $1
else
  grunt deps $1
fi

cd - >/dev/null
