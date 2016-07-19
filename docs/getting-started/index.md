# Developing

This guide is intended for those who wish to:

* Contribute code to Visallo
* Create their own Visallo plugins
* Create a custom build of Visallo

In order to work with Visallo as a developer, it's recommended that:

* You know Java, since the entire back end is written in Java.
* You know JavaScript, as the UI is very JavaScript heavy.

If that sounds like you, then please keep in mind the following conventions you'll encounter while reading this guide.

* Any reference to `$PROJECT_DIR` refers to the directory into which you've cloned the Visallo source code. Unless otherwise stated, all commands are run from this directory.
* Any reference to `$VISALLO_DIR` refers to the base directory for Visallo configuration. The most important subdirectory to this is the `config` directory, where Visallo configuration properties files placed.

## Install the Required [Dependencies](dependencies.md)

There are a few dependencies you'll need to install before developing with Visallo. This section will describe what needs to be installed.

## Get the [Source Code](source-code.md)

Before you can get started, you'll need to get a copy of the Visallo source code. This section explains how to do that
and a little about the source code structure.

## Get the Web Server [Running](running.md)

You'll need to run the web server to see all of the Visallo UI goodness. This section will show you how.

## Learn the [Build Options](build.md)

Visallo uses Maven as the build system. There are a number of build options depending on what you want to run. This
section will help you understand the options.

## Create an [Ontology](ontology.md)

Visallo uses OWL files to define what type of entities you can create, what properties they can have and what they
can connect to.

## [Getting Help](help.md)

Learn about your options for getting help in this section.

## [Contributing](contributing.md)

Once you've made changes that you want to share with the community, the next step is to submit those changes back via a pull request.
