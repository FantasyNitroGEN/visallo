#!/usr/bin/env bash

DIR=$(cd $(dirname "$0") && pwd)
VISALLO_DIR=${DIR}/..
ARCHETYPE_JAR_DIR=$VISALLO_DIR/archetype/target

cd ${VISALLO_DIR}

mvn clean package -am -pl archetype

cd ${DIR}

VERSION=$(find "${ARCHETYPE_JAR_DIR}" -name "visallo-plugin-archetype-*.jar" | sed -e 's/.*visallo-plugin-archetype-//' -e 's/\.jar$//')

mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
    -Dfile=${ARCHETYPE_JAR_DIR}/visallo-plugin-archetype-${VERSION}.jar \
    -DgroupId=com.visallo \
    -DartifactId=visallo-plugin-archetype \
    -Dversion=${VERSION} \
    -Dpackaging=jar \
    -DgeneratePom=true

mvn org.apache.maven.plugins:maven-archetype-plugin:2.4:crawl

mvn archetype:generate -DarchetypeGroupId=com.visallo -DarchetypeArtifactId=visallo-plugin-archetype -DarchetypeVersion=${VERSION}
