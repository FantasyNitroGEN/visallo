# Running Visallo

The steps below describe the quickest method for getting Visallo up and running in a development capacity. You'll eventually want to spend time understanding how to [create a custom ontology](ontology.md) and [develop Visallo extensions](../extension-points/index.md).


## Initialize the database

The following command will initialize the out-of-the-box embedded database. If you've configured a different database, you'll need to create it on your own.

      mvn -f dev/db/pom.xml sql:execute@create-db

## Run the Web Application

The following Maven command will run the web application. You may prefer to [run the web server using IntelliJ](../ide-setup/intellij.md) if you're doing active development.

      mvn -am -pl dev/jetty-server -P dev-jetty-run compile
