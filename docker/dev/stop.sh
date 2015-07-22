#!/bin/bash

function stop_msg {
  echo -e "\n\e[32mStopping $1\n---------------------------------------------------------------\e[0m"
}

stop_msg "RabbitMQ"
/opt/rabbitmq/sbin/rabbitmqctl stop

stop_msg "Elasticsearch"
curl -XPOST 'http://localhost:9200/_cluster/nodes/_local/_shutdown'
echo ""

stop_msg "Accumulo"
/opt/accumulo/bin/stop-all.sh

stop_msg "Hadoop"
/opt/hadoop/sbin/yarn-daemon.sh --config /opt/hadoop/etc/hadoop/ stop nodemanager
/opt/hadoop/sbin/yarn-daemon.sh --config /opt/hadoop/etc/hadoop/ stop resourcemanager
/opt/hadoop/sbin/hadoop-daemon.sh --config /opt/hadoop/etc/hadoop/ --script /opt/hadoop/sbin/hdfs stop datanode
/opt/hadoop/sbin/hadoop-daemon.sh --config /opt/hadoop/etc/hadoop/ --script /opt/hadoop/sbin/hdfs stop secondarynamenode
/opt/hadoop/sbin/hadoop-daemon.sh --config /opt/hadoop/etc/hadoop/ --script /opt/hadoop/sbin/hdfs stop namenode

stop_msg "ZooKeeper"
/opt/zookeeper/bin/zkServer.sh stop

echo ""
