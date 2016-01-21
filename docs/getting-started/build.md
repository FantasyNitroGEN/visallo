# Build Instructions

In the majority of cases, most Visallo components can be built by simply opening a terminal to the component root directory and running `mvn package`. The two most common components that are a little less straight-forward are the web application and yarn ingest components. Additional instructions for both can be found below.

<a name="root-module"/>
## Root Module Installation

You'll need to install the Visallo `root` Maven module after you've cloned the [source code](source-code.md) and any time you pull the latest Visallo source code.

        mvn install -f root/pom.xml

## Smoke Test

You should make sure everything compiles and tests pass before going any further. Otherwise, it's hard to reason about what might be wrong when things later fail.

Compile all modules:

    mvn compile

Run all unit tests, continuing on failures and without failing the build:

    mvn test -fn

It is a known issue that some unit tests fail on Windows. The following are expected to fail:
* `org.visallo.core.formula.FormulaEvaluatorTest`
* `org.visallo.tesseract.TesseractGraphPropertyWorkerTest`
* `org.visallo.opencvObjectDetector.OpenCVUtilsTest`
* `org.visallo.opencvObjectDetector.OpenCVObjectDetectorPropertyWorkerTest`

## Web Application

Building the Visallo web application is very similar to running it, as described in the [Running section of the development Docker container](dev-docker-image.md#running) instructions.

From the root directory of the Visallo project, run

        mvn package -pl web/war -am -DskipTests -Dsource.skip=true

The previous command will create a WAR file in the `web/war/target` directory.

<a name="web-plugin"/>
## Web Application Plugins

The Visallo web application can be extended with dynamically loaded plugins. You can find some example plugins in `web/plugins`.

To build a web plugin, run:

        mvn package -pl ./web/plugins/<plugin-name>/ -am -DskipTests

Once the plugin JAR file is created, copy it to the `/visallo/lib` directory in HDFS:

        hadoop fs -put <path_to_plugin_jar> /visallo/lib

Or, copy it to the web server's lib directory `/opt/visallo/lib` or for docker `docker/visallo-dev-persistent/opt/visallo/lib`.

This is the easiest way to expose the plugin to all web servers. The Visallo web application will automatically add the JAR file to the classpath.

To learn more about extending Visallo with plugins, please [read this](../extension-points/index.md).


## Graph Property Workers

Each graph property worker can be built independently using the following Maven command:

        mvn package -pl <path_to_plugin_dir> -am

Once the plugin JAR file is created, copy it to the `/visallo/lib` directory in HDFS.

        hadoop fs -put <path_to_plugin_jar> /visallo/lib

As an example, to build and deploy the `tika-mime-type-plugin` one would run the following commands from the root of
the Visallo project:

        mvn package -pl graph-property-worker/plugins/tika-mime-type -am
        hadoop fs -put graph-property-worker/plugins/tika-mime-type/target/visallo-tika-mime-*-jar-with-dependencies.jar /visallo/lib

Visallo's graph property worker YARN application  will automatically detect the plugin in the classpath.
