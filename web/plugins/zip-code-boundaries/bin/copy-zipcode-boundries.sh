#!/bin/bash

ZIP_URL=http://www2.census.gov/geo/tiger/GENZ2013/cb_2013_us_zcta510_500k.zip
HDFS_DIR=/visallo/config/org.visallo.zipCodeBoundaries.ZipCodeBoundariesRepository

hdfs dfs -ls ${HDFS_DIR} &> /dev/null
if [ $? -ne 0 ]; then
  unzipped_dir=$(basename ${ZIP_URL} .zip)
  if [ ! -d ${unzipped_dir} ]; then
    echo "downloading ${ZIP_URL}..."
    curl -f -s -L ${ZIP_URL} -O
    mkdir ${unzipped_dir}
    (cd ${unzipped_dir} && unzip -q ../$(basename ${ZIP_URL}))
  fi
  echo "uploading files to ${HDFS_DIR}..."
  hdfs dfs -mkdir -p ${HDFS_DIR}
  hdfs dfs -put ${unzipped_dir}/* ${HDFS_DIR}
fi
