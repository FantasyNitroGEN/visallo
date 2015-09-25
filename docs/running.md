# Running Visallo

This guide covers running Visallo within the development Docker container. The steps below describe the quickest method for getting Visallo up and running in a development capacity. You'll eventually want to spend time understanding how to [create a custom ontology](ontology.md) and [develop Visallo extensions](extension-points.md).

## Install Accumulo Iterators

Run the following commands in the `/opt/visallo-source` directory within the development Docker container:

        mvn compile -pl docker
        /opt/accumulo/bin/stop-all.sh
        /opt/accumulo/bin/start-all.sh

## Deploy an Ontology

An ontology must be loaded prior to running Visallo. The source code for the project comes with sample ontologies to use as a starting point. The following command will load the minimal ontology to get you up and running quickly. It's important to note that the minimal ontology is very sparse, and you won't be able to do too much in Visallo with it. When you're ready to dig into Visallo a little deeper, you'll want to read the [section on ontologies](ontology.md).

Run the following command from the `/opt/visallo-source` directory within the development Docker container:

        mvn compile -am -pl tools/cli \
            -P run-cli \
            -Dexec.args='OwlImport --in examples/ontology-minimal/minimal.owl'


## Run the Web Application

Run the commands below to start the Visallo web application. These steps must be run from the development Docker container shell resulting from running the `docker/run-dev.sh` script.

First, build the web application. This only needs to be run once:

        mvn -am -pl web/war \
            -Dgrunt.target=development \
            compile

Then, run the web application using Jetty:

        mvn -am -pl web/war \
            -P-build-webapp,jetty-run,web-admin,web-auth-username-only \
            compile

The preceding `mvn` command will start the Visallo web application with a minimum number of features running. The `-P` option to the Maven command above specifies which profiles are included when starting Jetty. A profile groups a set of dependencies that make up a feature. Running the following command will list all of the available profiles that can be run.

        mvn -am -pl web/war help:all-profiles

Profiles that begin with `gpw` are graph property workers, which are primarily features that process ingested data. Features starting with `web` are web application plugins.

It's also worth noting that some profiles are configured to run automatically. You can run the following command to see which profiles they are.

        mvn -am -pl web/war help:active-profiles
