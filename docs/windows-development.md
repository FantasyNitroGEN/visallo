# Developing Visallo on Windows (x64 only)

**Please use a Windows user account which does not have spaces in its home directory.**

## Environment Setup

1. Install Google Chrome browser: https://www.google.com/chrome/

1. Install Docker Toolbox: [http://docs.docker.com/windows/started/](http://docs.docker.com/windows/started/). Select the full installation and let the installer add the executables to the system PATH variable. Please follow Step 1 and Step 2 of the window docker toolbox instructions.

1. Install MinGW: [http://sourceforge.net/projects/mingw/files/Installer/mingw-get-setup.exe/download](http://sourceforge.net/projects/mingw/files/Installer/mingw-get-setup.exe/download). Use the default `C:\MinGW` directory. When the MinGW Installation Manager appears, select "All Packages" and install only the the `mingw32-make` bin package.

1. Install Python v2.7.x: [https://www.python.org/downloads/](https://www.python.org/downloads/). Install it to `C:\Python27`.

1. Install JDK 8: [http://www.oracle.com/technetwork/java/javase/downloads/index.html](http://www.oracle.com/technetwork/java/javase/downloads/index.html). Change the JDK installation directory to `C:\jdk1.8.0_60`. Use the default installation directory for the subsequent JRE installation.

1. Install Maven 3.3.x: [http://maven.apache.org/guides/getting-started/windows-prerequisites.html](http://maven.apache.org/guides/getting-started/windows-prerequisites.html). Download the binary ZIP archive and extract it to `C:\apache-maven-3.3.3`.

1. Install Hadoop 2.6.0 from the pre-compiled binaries: [http://bits.v5analytics.com/static/hadoop-windows-x64-2.6.0.zip](http://bits.v5analytics.com/static/hadoop-windows-x64-2.6.0.zip). Extract the archive to `C:\hadoop-2.6.0`

1. Open up a Git Bash window. (This was installed by Docker Toolbox.) Your current directory will be your Windows home directory. The remaining commands in these instructions should be run from within this window.

1. Copy the MinGW make command so it's found later by the grunt tasks:

         cp /c/MinGW/bin/mingw32-make.exe /c/MinGW/bin/make.exe

1. Add the following lines to the file `.bash_profile` in your home directory. Use `vim` to create and edit this file. Adjust the installation directories accordingly if you selected any different locations.

        export JAVA_HOME=/c/jdk1.8.0_60
        export HADOOP_HOME=/c/hadoop-2.6.0
        export PATH=$JAVA_HOME/bin:/c/apache-maven-3.3.3/bin:$HADOOP_HOME/bin:/c/Python27:/c/MinGW/bin:$PATH

        alias grunt="$HOME/visallo/web/war/node/node $HOME/visallo/web/war/src/main/webapp/node_modules/grunt-cli/bin/grunt"

1. Exit the Git Bash window and open a new one. The above variables should be set. Verify by typing `env`.

1. Create the Docker Machine VM:

        docker-machine create  \
            --driver virtualbox \
            --virtualbox-memory 8192 \
            --virtualbox-boot2docker-url https://github.com/boot2docker/boot2docker/releases/download/v1.8.1/boot2docker.iso \
            visallo-dev

1. Get the VM IP address:

        docker-machine ip visallo-dev

1. Edit `C:\Windows\System32\drivers\etc\hosts` as an administrator, adding a new line with the VM IP address:

        [docker machine ip] visallo-dev

1. Clone the Visallo git repo under your home directory:

   *Before doing this, it would be a good idea to read about the [`core.autocrlf`](https://git-scm.com/book/en/v2/Customizing-Git-Git-Configuration) configuration setting, and determine what will work best for your environment.*

        cd $HOME
        git clone https://github.com/v5analytics/visallo.git

# Building Visallo

Always run build commands from the project home directory:

    cd $HOME/visallo

Install the root POM. This should be run whenever you pull the latest Visallo source code:

    mvn -f root/pom.xml install

Compile all modules, building the web app resources for development (which is faster):

    mvn compile -Dgrunt.target=development

Run all unit tests, continuing on failures and without failing the build:

    mvn test -fn -Dgrunt.target=development

It is a known issue that some unit tests fail on Windows. The following are expected to fail:
* `org.visallo.core.formula.FormulaEvaluatorTest`
* `org.visallo.tesseract.TesseractGraphPropertyWorkerTest`
* `org.visallo.opencvObjectDetector.OpenCVUtilsTest`
* `org.visallo.opencvObjectDetector.OpenCVObjectDetectorPropertyWorkerTest`

# Starting the Docker Container

1. SSH into the docker-machine VM:

        docker-machine ssh visallo-dev

1. (Inside of the docker-machine VM) Change directory to your Visallo repository:

        cd /c/Users/[username]/visallo

1. (Inside of the docker-machine VM) Pull the docker image:

   *This only needs to be run the first time, or when a the docker configuration changes (files in the `docker` directory).*

        docker pull visallo/dev

1. (Inside of the docker-machine VM) Run the docker image (this will start ZooKeeper, HDFS, YARN, ElasticSearch, and RabbitMQ):

        docker/run-dev.sh

Don't exit the docker container until you want to stop these services.

# Running Visallo

Maven can be used to execute various command-line tools and the Visallo web application. Always run from the project directory:

    cd $HOME/visallo

## Format

Run this the first time, and whenever you need to clear all data or load a new ontology.

    mvn compile -am -pl tools/cli \
        -P run-cli,storage-accumulo,search-elasticsearch,queue-rabbitmq,acl-ontology \
        -Dexec.args='FormatVisallo'

## Ontology

The following command loads an ontology that works well for general development:

    mvn compile -am -pl tools/cli \
        -P run-cli,storage-accumulo,search-elasticsearch,queue-rabbitmq,acl-ontology \
        -Dexec.args='OwlImport --in examples/ontology-dev/dev.owl'

## Web Server

This command runs Visallo using the Jetty web server, with a username-only login, and with the backed services provided by the development docker container:

    mvn compile -am -pl web/war \
        -P jetty-run,web-admin,web-auth-username-only,storage-accumulo,search-elasticsearch,queue-rabbitmq,acl-ontology \
        -Dgrunt.target=development

Use Chrome or Firefox to browse to `http://127.0.0.1:8443`. Internet Explorer is not currently supported.

If you want to modify JavaScript and CSS/Less without recompiling and restarting the web server each time, run the following commands in another Git Bash window:

    cd $HOME/visallo/web/war/src/main/webapp
    grunt

Refresh the Visallo web page to reload any changes.

