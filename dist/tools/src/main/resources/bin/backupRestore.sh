#!/bin/bash

DIR=$(cd $(dirname "$0") && pwd)
CP=$(find ${DIR}/../lib -type f | paste -s -d ':' -)

java -cp ${CP} org.visallo.backupRestore.BackupRestore "$@"
