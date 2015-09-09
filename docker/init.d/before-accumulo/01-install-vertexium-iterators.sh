#!/bin/bash

pattern=(/opt/visallo/lib/vertexium-accumulo-iterators-*.jar)
if [ ! -f ${pattern[0]} ]; then
  mvn compile -pl docker
fi
