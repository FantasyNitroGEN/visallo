
# Configuration

## Recommended Initial Setup
There are several required configuration properties that must be specified before starting Visallo.

An initial configuration file with default configuration values can be found [here](../config/visallo.properties).
Additionally, a default application logging configuration is available [here](../config/log4j.xml).

These files must be present in one of the [configuration search locations](##Configuration search order).

## Configuration search order

By default Visallo will use `org.visallo.core.config.FileConfigurationLoader` to load configuration files.
`FileConfigurationLoader` will look in the following directories:

* `/opt/visallo/` for Linux/OSX
* `c:/opt/visallo/` for Windows
* `${appdata}/Visallo`
* `${user.home}/.visallo`
* Directory specified by the environment variable `VISALLO_DIR`

Each of these directories will be searched in order and all files with a `.properties` extension will be
read in alphabetic order. This allows you to override properties in various places.

## Docker

If you are running Visallo processes in Docker the same configuration loading will occur but within the docker
container. This directory is exposed in the `docker/visallo-dev-persistent/opt/visallo` under your visallo source
tree.
