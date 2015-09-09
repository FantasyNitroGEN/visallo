#!/bin/bash

INIT_D=/opt/visallo-source/docker/init.d

function _msg {
  echo -e "\n\e[32m$1\n---------------------------------------------------------------\e[0m"
}

function start_msg {
  _msg "Starting $1"
}

function _initd {
  local dir=${INIT_D}/$1
  if [ -d "${dir}" ]; then
    _msg "running scripts in ${dir}..."
    for script in ${dir}/*; do
      if [ -x "${script}" ]; then
        echo ${script}
        script=$(readlink -f ${script})
        ${script}
      fi
    done
    echo
  fi
}

function start_zookeeper {
  _initd before-zookeeper
  start_msg "ZooKeeper"
  /opt/zookeeper/bin/zkServer.sh start
  _initd after-zookeeper
}

function start_hadoop {
  _initd before-hdfs
  start_msg "Hadoop"
  sed s/HOSTNAME/$HOSTNAME/ /opt/hadoop/etc/hadoop/core-site.xml.template > /opt/hadoop/etc/hadoop/core-site.xml
  mkdir -p /var/log/hadoop

  if [ ! -d "/tmp/hadoop-root" ]; then
    echo "**************** FORMATING NAMENODE ****************"
    /opt/hadoop/bin/hdfs namenode -format
  fi
  /opt/hadoop/sbin/hadoop-daemon.sh --config /opt/hadoop/etc/hadoop/ --script /opt/hadoop/sbin/hdfs start namenode
  /opt/hadoop/sbin/hadoop-daemon.sh --config /opt/hadoop/etc/hadoop/ --script /opt/hadoop/sbin/hdfs start secondarynamenode
  /opt/hadoop/sbin/hadoop-daemon.sh --config /opt/hadoop/etc/hadoop/ --script /opt/hadoop/sbin/hdfs start datanode
  _initd after-hdfs
  _initd before-yarn
  /opt/hadoop/sbin/yarn-daemon.sh --config /opt/hadoop/etc/hadoop/ start resourcemanager
  /opt/hadoop/sbin/yarn-daemon.sh --config /opt/hadoop/etc/hadoop/ start nodemanager
  _initd after-yarn
  /opt/hadoop/bin/hdfs dfsadmin -safemode wait
}

function start_accumulo {
  _initd before-accumulo
  start_msg "Accumulo"
  echo $HOSTNAME > /opt/accumulo/conf/masters
  echo $HOSTNAME > /opt/accumulo/conf/slaves
  echo $HOSTNAME > /opt/accumulo/conf/tracers
  echo $HOSTNAME > /opt/accumulo/conf/gc
  echo $HOSTNAME > /opt/accumulo/conf/monitor
  mkdir -p /var/log/accumulo

  if [ $(/opt/hadoop/bin/hadoop fs -ls /user | grep accumulo | wc -l) == "0" ]; then
    echo "Creating accumulo user in hdfs"
    /opt/hadoop/bin/hadoop fs -mkdir -p /user/accumulo
    /opt/hadoop/bin/hadoop fs -chown accumulo /user/accumulo
  fi

  if /opt/accumulo/bin/accumulo info 2>&1 | grep --quiet "Accumulo not initialized"; then
    echo "**************** INITIALIZING ACCUMULO ****************"
    /opt/accumulo/bin/accumulo init --instance-name visallo --password password --clear-instance-name
  fi
  /opt/accumulo/bin/start-all.sh
  _initd after-accumulo
}

function start_elasticsearch {
  _initd before-elasticsearch
  start_msg "Elasticsearch"
  mkdir -p /var/log/elasticsearch

  /opt/elasticsearch/bin/elasticsearch > /dev/null &
  _initd after-elasticsearch
}

function start_rabbitmq {
  _initd before-rabbitmq
  start_msg "RabbitMQ"
  /opt/rabbitmq/sbin/rabbitmq-plugins --offline enable rabbitmq_management
  /opt/rabbitmq/sbin/rabbitmq-server > /dev/null &
  _initd after-rabbitmq
}

function ensure_visallo_config {
  start_msg "Visallo Config"
  hadoop fs -mkdir -p /visallo/lib
  hadoop fs -mkdir -p /visallo/config/opencv
  hadoop fs -mkdir -p /visallo/config/opennlp
  hadoop fs -put /opt/visallo-source/config/opencv/haarcascade_frontalface_alt.xml /visallo/config/opencv/
  hadoop fs -put /opt/visallo-source/config/opennlp/* /visallo/config/opennlp/
  hadoop fs -chmod -R a+w /visallo/
}

start_zookeeper
start_hadoop
start_accumulo
start_elasticsearch
start_rabbitmq
ensure_visallo_config

if [ $PPID -eq 1 ]; then
  /bin/bash
fi
