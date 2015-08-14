
FROM centos:centos7

# Install RPM packages
ADD scripts/install-rpm-packages.sh /opt/visallo/scripts/install-rpm-packages.sh
RUN /bin/bash /opt/visallo/scripts/install-rpm-packages.sh

# Install NPM packages
ADD scripts/install-npm-packages.sh /opt/visallo/scripts/install-npm-packages.sh
RUN /bin/bash /opt/visallo/scripts/install-npm-packages.sh

# Passwordless SSH
ADD scripts/install-ssh.sh /opt/visallo/scripts/install-ssh.sh
ADD ssh_config /root/.ssh/config
RUN /bin/bash /opt/visallo/scripts/install-ssh.sh
EXPOSE 22

# Install Java
ADD scripts/install-java.sh /opt/visallo/scripts/install-java.sh
ADD java/java.sh /etc/profile.d/java.sh
ENV PATH $PATH:/opt/jdk/bin
ENV JAVA_HOME /opt/jdk
ENV _JAVA_OPTIONS -Djava.net.preferIPv4Stack=true
RUN /bin/bash /opt/visallo/scripts/install-java.sh

# Install Maven
ADD scripts/install-maven.sh /opt/visallo/scripts/install-maven.sh
ADD maven/settings.xml /root/.m2/settings.xml
ENV PATH $PATH:/opt/maven/bin
ENV MVN_HOME /opt/maven
RUN /bin/bash /opt/visallo/scripts/install-maven.sh
VOLUME ["/opt/maven/repository"]

# Install ZooKeeper
ADD scripts/install-zookeeper.sh /opt/visallo/scripts/install-zookeeper.sh
ENV PATH $PATH:/opt/zookeeper/bin
ENV ZOOKEEPER_HOME /opt/zookeeper
RUN /bin/bash /opt/visallo/scripts/install-zookeeper.sh
EXPOSE 2181 2888 3888

# Install Hadoop
ENV HADOOP_PREFIX /opt/hadoop
ENV HADOOP_COMMON_HOME /opt/hadoop
ENV HADOOP_HDFS_HOME /opt/hadoop
ENV HADOOP_MAPRED_HOME /opt/hadoop
ENV HADOOP_YARN_HOME /opt/hadoop
ENV HADOOP_CONF_DIR /opt/hadoop/etc/hadoop
ENV YARN_CONF_DIR /opt/hadoop/etc/hadoop
ENV PATH $PATH:/opt/hadoop/bin
ADD scripts/install-hadoop.sh /opt/visallo/scripts/install-hadoop.sh
ADD hadoop/core-site.xml.template /opt/hadoop-2.6.0/etc/hadoop/core-site.xml.template
RUN /bin/bash /opt/visallo/scripts/install-hadoop.sh
ADD hadoop/hdfs-site.xml /opt/hadoop-2.6.0/etc/hadoop/hdfs-site.xml
ADD hadoop/mapred-site.xml /opt/hadoop-2.6.0/etc/hadoop/mapred-site.xml
ADD hadoop/yarn-site.xml /opt/hadoop-2.6.0/etc/hadoop/yarn-site.xml
VOLUME ["/var/lib/hadoop-hdfs", "/var/local/hadoop"]
EXPOSE 8020 8032 8088 9000 50010 50020 50030 50060 50070 50075 50090

# Install Accumulo
ADD scripts/install-accumulo.sh /opt/visallo/scripts/install-accumulo.sh
ENV PATH $PATH:/opt/accumulo/bin
RUN /bin/bash /opt/visallo/scripts/install-accumulo.sh
EXPOSE 9997 9999 50091 50095

# Install ElasticSearch
ADD scripts/install-elasticsearch.sh /opt/visallo/scripts/install-elasticsearch.sh
ENV PATH $PATH:/opt/elasticsearch/bin
RUN /bin/bash /opt/visallo/scripts/install-elasticsearch.sh
ADD elasticsearch/elasticsearch.yml /opt/elasticsearch/config/elasticsearch.yml
VOLUME ["/opt/elasticsearch-1.7.0/data/"]
EXPOSE 9200 9300

# Install RabbitMQ
ADD scripts/install-rabbitmq.sh /opt/visallo/scripts/install-rabbitmq.sh
ENV PATH $PATH:/opt/rabbitmq/sbin
RUN /bin/bash /opt/visallo/scripts/install-rabbitmq.sh
ADD rabbitmq/etc/rabbitmq/rabbitmq.config /opt/rabbitmq_server-3.5.0/etc/rabbitmq/rabbitmq.config
VOLUME ["/opt/rabbitmq_server-3.5.0/var"]
EXPOSE 5672 5673 15672

# Install Jetty
ADD scripts/install-jetty.sh /opt/visallo/scripts/install-jetty.sh
RUN /bin/bash /opt/visallo/scripts/install-jetty.sh
ADD jetty/start.ini /opt/jetty/start.ini
ADD jetty/jetty-logging.properties /opt/jetty/resources/jetty-logging.properties
ADD jetty/jetty.xml /opt/jetty/etc/jetty.xml
ADD jetty/jetty-http.xml /opt/jetty/etc/jetty-http.xml
ADD jetty/jetty-https.xml /opt/jetty/etc/jetty-https.xml
ADD jetty/jetty-ssl.xml /opt/jetty/etc/jetty-ssl.xml
ADD jetty/jetty.jks /opt/jetty/etc/jetty.jks
ENV PATH $PATH:/opt/jetty/bin
VOLUME ["/opt/jetty/webapps"]
EXPOSE 8080 8443

# Install .bowerrc
ADD bower/dot-bowerrc /root/.bowerrc

# add scripts
ADD docker-entrypoint.sh /opt/docker-entrypoint.sh
ADD start.sh /opt/start.sh
ADD stop.sh /opt/stop.sh
ADD status.sh /opt/status.sh

RUN \
  chmod +x /opt/start.sh \
  && chmod +x /opt/stop.sh \
  && chmod +x /opt/status.sh \
  && chmod +x /opt/docker-entrypoint.sh

# Set working environment
VOLUME ["/tmp", "/var/log", "/opt/visallo", "/opt/visallo-source"]
WORKDIR /opt/visallo-source
ENTRYPOINT ["/opt/docker-entrypoint.sh"]
CMD ["start"]
