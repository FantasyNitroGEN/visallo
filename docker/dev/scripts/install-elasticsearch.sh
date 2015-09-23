#!/bin/bash -eu

wget -O /opt/elasticsearch-1.7.2.tar.gz https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.7.2.tar.gz
tar -xzf /opt/elasticsearch-1.7.2.tar.gz -C /opt
rm /opt/elasticsearch-1.7.2.tar.gz
ln -s /opt/elasticsearch-1.7.2 /opt/elasticsearch
rm -rf /opt/elasticsearch-1.7.2/logs
mkdir -p /var/log/elasticsearch
ln -s /var/log/elasticsearch /opt/elasticsearch-1.7.2/logs
/opt/elasticsearch/bin/plugin -install mobz/elasticsearch-head
