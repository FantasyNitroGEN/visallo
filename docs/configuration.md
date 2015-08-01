
# Configuration

## Recommended Initial Setup

There are several required configuration properties that must be specified before starting Visallo.

An [example configuration file](../config/visallo.properties) with default configuration values and
an [example logging configuration](../config/log4j.xml) are provided in the `/config` directory.

These files must be copied (or symlinked) into a `$VISALLO_DIR/config` directory.


## Search Order for `.properties` and `.jar` Files

By default Visallo will use `org.visallo.core.config.FileConfigurationLoader` to load configuration files
and `org.visallo.core.bootstrap.lib.LibDirectoryLoader` to load additional `.jar` files.

The following directories will be searched in order:

* `/opt/visallo/` for Linux/OSX
* `c:/opt/visallo/` for Windows
* `${appdata}/Visallo`
* `${user.home}/.visallo`
* a directory specified with the `VISALLO_DIR` environment variable

All files in `/config` subdirectories with `.properties` extensions will then be loaded alphabetically.
allowing you to override properties in various places.

All `.jar` files in `/lib` subdirectories will be added to the classpath.


## Docker

If you are running Visallo processes in Docker the same configuration loading will occur but within the docker
container. This directory is exposed in the `docker/visallo-dev-persistent/opt/visallo` under your visallo source
tree.
