# Running Visallo

The instructions below describe the quickest method for getting Visallo up and running in a development capacity. You'll eventually want to spend time understanding how to [create a custom ontology](ontology.md) and [develop Visallo extensions](../extension-points/index.md).

## Run the Web Application

The following Maven command will run the web application. You may prefer to [run the web server using IntelliJ](../ide-setup/intellij.md) if you're doing active development. This command will also initialize the out-of-box embedded database.

    mvn -am -pl dev/tomcat-server -P dev-tomcat-run compile

Once the log output stops, your server will be available at [http://localhost:8888](http://localhost:8888).
