#!/bin/bash -eu

DIR=$(cd $(dirname $0) && pwd)

cd ${DIR}/../root
mvn install "$@"

cd ${DIR}/..
mvn clean package -DskipTests -Dmaven.test.skip=true -am -pl web/war,dist/all "$@"

cd ${DIR}/../dist/all/target
tar -czf visallo-dist-$(date +'%Y%m%d').tgz dist
ls -lh ${DIR}/../dist/all/target/*.tgz
