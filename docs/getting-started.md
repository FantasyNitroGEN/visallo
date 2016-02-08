# Getting Started

While we recommend reading all the pages in this Developer Guide, the steps below are the fastest path to getting an instance of Visallo up and running in no time. Please make sure you have all [required dependencies](dependencies.md) installed before attempting any of the steps below.

Clone the source code.

      git clone git://github.com/v5analytics/visallo.git

Change directories to the checked out code. This is your `$PROJECT_DIR` directory.

      cd visallo

Initialize the out-of-the-box embedded database.

      mvn -f dev/db/pom.xml sql:execute@create-db

Run the web application.

      mvn -am -pl dev/jetty-server -P dev-jetty-run compile

