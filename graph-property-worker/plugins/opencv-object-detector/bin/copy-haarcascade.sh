#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
HDFS_DIR=/visallo/config/org.visallo.opencvObjectDetector.OpenCVObjectDetectorPropertyWorker

hdfs dfs -ls ${HDFS_DIR} &> /dev/null
if [ $? -ne 0 ]; then
  hdfs dfs -mkdir -p ${HDFS_DIR}
  hdfs dfs -put ${DIR}/../src/main/resources/haarcascade_frontalface_alt.xml ${HDFS_DIR}
fi