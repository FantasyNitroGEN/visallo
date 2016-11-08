#!/bin/bash -eu

# Build the archetype and install to local Maven repo. This is the only way for the test project to be generated.
mvn clean install

# Compile and test the generated test project.
mvn -f target/test-classes/projects/basic/project/basic/pom.xml test
