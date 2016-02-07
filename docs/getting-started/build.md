# Build Instructions

In the majority of cases, most Visallo components can be built by simply opening a terminal to the component root directory and running `mvn package`. More specific instructions for both can be found below.

<a name="root-module"/>
## Root Module Installation

You'll need to install the Visallo `root` Maven module after you've cloned the [source code](source-code.md) and any time you pull the latest Visallo source code.

        mvn install -f root/pom.xml

## Database Initialization

You will need to create the necessary tables before using the default out-of-the-box embedded database. This can be done by running the following maven command from the `$PROJECT_DIR` directory.

      mvn -f dev/db/pom.xml sql:execute@create-db

## Smoke Test

You should make sure everything compiles and tests pass before going any further. Otherwise, it's hard to reason about what might be wrong when things later fail.

Compile all modules:

    mvn compile

Run all unit tests, continuing on failures and without failing the build:

    mvn test -fn

It is a known issue that some unit tests fail on Windows. The following are expected to fail:
* `org.visallo.core.formula.FormulaEvaluatorTest`

## Web Application

Building the Visallo web application is very similar to running it. From the root directory of the Visallo project, run

        mvn package -pl web/war -am -DskipTests -Dsource.skip=true

The previous command will create a WAR file in the `web/war/target` directory.

<a name="web-plugin"/>
## Web Application Plugins

The Visallo web application can be extended with dynamically loaded plugins. You can find some example plugins in `web/plugins`.

To build a web plugin, run:

        mvn package -pl ./web/plugins/<plugin-name>/ -am -DskipTests

Once the plugin JAR file is created, copy it to `$VISALLO_DIR/lib`, which should be accessible on all of your servers.

This is the easiest way to expose the plugin to all web servers. The Visallo web application will automatically add the JAR file to the classpath.

To learn more about extending Visallo with plugins, please [read this](../extension-points/index.md).


## Graph Property Workers

Each graph property worker can be built independently using the following Maven command:

        mvn package -pl <path_to_plugin_dir> -am

Once the plugin JAR file is created, copy it to `$VISALLO_DIR/lib`, which should be accessible on all of your servers.

As an example, to build and deploy the `tika-mime-type-plugin` one would run the following commands from the root of
the Visallo project:

        mvn package -pl graph-property-worker/plugins/tika-mime-type -am
        cp graph-property-worker/plugins/tika-mime-type/target/visallo-tika-mime-*-jar-with-dependencies.jar $VISALLO_DIR/lib

Visallo's will automatically detect graph property workers found within the classpath.
