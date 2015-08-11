#!/bin/bash -eu

rpm -Uhv http://dl.fedoraproject.org/pub/epel/7/x86_64/e/epel-release-7-5.noarch.rpm
yum update -y
yum install -y wget curl tar sudo openssh-server openssh-clients git nodejs npm libuuid-devel libtool zip unzip rsync which erlang bzip2
