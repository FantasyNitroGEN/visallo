# Running Visallo

This guide covers running Visallo within the development Docker container. The steps below describe the quickest method for getting Visallo up and running in a development capacity. You'll eventually want to spend time understanding how to [create a custom ontology](ontology.md) and [develop Visallo extensions](extension-points.md).

## Install Accumulo Iterators

Run the following commands in the `/opt/visallo-source` directory within the development Docker container:

        mvn compile -pl docker
        /opt/stop.sh
        /opt/start.sh

## Deploy an Ontology

An ontology must be loaded prior to running Visallo. The source code for the project comes with sample ontologies to use as a starting point. The following command will load the minimal ontology to get you up and running quickly. It's important to note that the minimal ontology is very sparse, and you won't be able to do too much in Visallo with it. When you're ready to dig into Visallo a little deeper, you'll want to read the [section on ontologies](ontology.md).

Run the following command from the `/opt/visallo-source` directory within the development Docker container:

        mvn -f tools/cli \
            -P storage-accumulo,search-elasticsearch,queue-rabbitmq \
            exec:exec \
            -Dexec.executable=java \
            -Dexec.args='-classpath %classpath org.visallo.core.cmdline.OwlImport --in /opt/visallo-source/examples/ontology-minimal/minimal.owl'

## Run the Web Application

Run the commands below to start the Visallo web application. These steps must be run from the development Docker container shell resulting from running the `docker/run-dev.sh` script.

        mvn -f web/war \
            -P storage-accumulo,search-elasticsearch,queue-rabbitmq,web-admin,web-auth-username-only \
            jetty:run

The preceding `mvn` command will start the Visallo web application with a minimum number of features running. The `-P` option to the Maven command above specifies which profiles are included when starting Jetty. A profile groups a set of dependencies that make up a feature. Running the following command will list all of the available profiles that can be run.

        mvn -f web/war \
            help:all-profiles
