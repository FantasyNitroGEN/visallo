# Visallo Quick-Start

This guide covers running Visallo as a all-in-one Java application for evaluation and demonstration purposes with small to medium data sets. This is not intended for development or production use. There are pros and cons to running this way:

PROS:
* Uses an embedded H2 database, in-process Elastic Search, and other in-memory services, so there are no external dependencies.
* Runs on Linux, Windows, and OS X systems by double-clicking a single jar file.
* Includes graph property workers for file MIME type detection, text extraction from documents, and OpenNLP for entity resolution.

CONS:
* The quick-start ontology is limited to only a few concept types and no relationships.
* Importing data can only be done by dragging files to the graph, or with RDF triple import files.
* Will not handle very large data sets since there's only a single instance of Elastic Search running.

## Building

From the root project directory, use the following command. This only needs to be run once:

        mvn -am -pl dev/quick-start package -DskipTests

The application jar file will be built at `dev/quick-start/target/visallo-dev-quick-start-*-SNAPSHOT.jar`.

## Running

There are two ways to run:

1. From the root project directory, use the following command:

        java -jar dev/quick-start/target/visallo-dev-quick-start-*-SNAPSHOT.jar

   Running this way sends output to the console.

2. Double-click the above jar file. It can be copied to any system with Java 7 or 8 installed. A console window will appear.

However you run, wait for the message "Listening on http port 8080 and https port 8443" to appear in the console. Then point your browser to `https://localhost:8443/`, and accept any certificate security exceptions when prompted.

## Customization

It is possible to create a custom quick-start configuration that uses a different ontology or runs additional graph property works. [Contact V5 Analytics](http://visallo.org/services) for assistance.
