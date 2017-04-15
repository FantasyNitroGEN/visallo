# Source Code

Visallo is hosted at [GitHub](http://www.github.com) and uses [Git](http://git-scm.com/) for source control. In order to
obtain the source code, you must first install Git on your system. GitHub provides
[instructions for installing and setting up Git](https://help.github.com/articles/set-up-git).

To get started, clone the main repository using this command. 

    git clone git://github.com/visallo/visallo.git

If you're planning on contributing to Visallo, then it's a good idea to fork the repository first. GitHub provides [instructions for forking a repository](https://help.github.com/articles/fork-a-repo). After forking the Visallo repository, you'll want to create a local clone of your fork in which to make changes before [creating a pull request](https://help.github.com/articles/creating-a-pull-request/).

## Directory Structure

The Visallo directory and file structure is as follows. Within these directories and their children you'll often find `README.md` files with more specific information.

* `common` - common code shared among multiple Visallo components
* `config` - configuration files for various Visallo components
* `core` - core components used throughout Visallo
* `dev` - components to facilitate Visallo development
* `docs` - Visallo documentation, like the page you're reading now
* `examples` - examples demonstrating the use of Visallo
* `graph-property-worker` - all of Visallo's graph property worker related code
  * `plugins` - the guts of all ingest and processing/analytics
  * `graph-property-worker-plugin-base` - core graph property worker classes used by GPW plugins
* `root` - Maven root POM. `mvn install` this early and often
* `tools` - command-line utilities
* `web` - everything related to Visallo's webapp
  * `client-api` - generated Java API for interacting with Visallo
  * `client-api-codegen` - utility for generating the client API
  * `plugins` - optional webapp plugins, including authentication plugins (one required)
  * `server` - convenience classes for running webapp in-process (e.g. within IDE)
  * `war` - front-end code for the webapp (javascript, css, images, etc.)
  * `web-base` - core route processing code
  * `web-plugin-base` - core web plugin classes used by web plugins
