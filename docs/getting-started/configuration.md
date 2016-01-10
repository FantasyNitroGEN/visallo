
# Configuration

## Inside Docker Container

If you are running Visallo processes in Docker using `docker/run-dev.sh` no additional steps are required, `run-dev.sh` will copy the example files to `docker/visallo-dev-persistent/opt/visallo/config` which is mapped to `/opt/visallo/config` inside the Docker container.

## Outside Docker Container

If you would like to run and develop Visallo outside of docker there are several required configuration properties that must be specified before starting Visallo.

If you are running the dev docker container you will need to copy (or symlink) the following configuration files from the `/config` directory into one of the directories mentioned below (`/opt/visallo` is recommended).

        log4j.xml
        visallo.properties
        visallo-accumulo.properties
        visallo-development.properties
        visallo-elasticsearch.properties
        visallo-hadoop.properties
        visallo-rabbitmq.properties
        visallo-webapp.properties
        visallo-zookeeper.properties

These files must be copied (or symlinked) into a `${VISALLO_DIR}/config` directory.


## Search Order for `.properties` and `.jar` Files

By default Visallo will use `org.visallo.core.config.FileConfigurationLoader` to load configuration files and `org.visallo.core.bootstrap.lib.LibDirectoryLoader` to load additional `.jar` files.

The following directories will be searched in order:

* `/opt/visallo/` for Linux/OS X
* `c:/opt/visallo/` for Windows
* `${appdata}/Visallo`
* `${user.home}/.visallo`
* a directory specified with the `VISALLO_DIR` environment variable

All files in `/config` subdirectories with `.properties` extensions will then be loaded alphabetically allowing you to override properties in various places.

All `.jar` files in `/lib` subdirectories will be added to the classpath.
