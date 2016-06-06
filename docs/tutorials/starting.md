# Starting Development

## Prerequisites

Ensure that you have the [dependencies](../getting-started/dependencies.md) installed.  Since subsequent tutorials will have some software development involved, I recommend that you have a Java IDE like Intellij.  For this tutorial, you should be somewhat familiar with the command line and maven.  That being said, we will do our best to make the instructions as clear as possible for someone who isn't adept with either of those.

## Background

One of the major components of Visallo is the system of Graph Property Workers that enhance and do analytics on top of the data.  Since most organizations will have different use-cases for their own data, the graph property workers are designed to be as extensible as possible.  For more information on graph property workers, I recommend [reading the documentation about Graph Property Workers](../extension-points/back-end/graphpropertyworkers.md) before beginning this tutorial, but it is not required.  

## Getting Started

We will be using maven archetypes to do most of the bootstrapping of Visallo so that we can only focus on developing our graph property worker.

<div class="alert alert-warning">
  Please contact us at <a href='https://www.visallo.com/content/contact'>visallo.com</a> in order to receive the archetype jar and instructions.  The archetype jar will allow you to complete all of the tutorials.
</div>

* Run the following command in a directory of your choosing:

```bash
mvn archetype:generate -DarchetypeGroupId=com.visallo -DarchetypeArtifactId=visallo-plugin-archetype
```

Maven archetype will ask you a couple of questions:

* for groupId, put in ```com.visalloexample.helloworld```
* for artifactId, put in ```visallo-helloworld```
* hit enter to accept the defaults for version, package, and ontologyBaseIri

Great!  You have your project set up for development.  At this point, cd into the directory ```visallo-helloworld```

Now run ```mvn package```.  We need to do this so that maven will download all of our dependencies.  This could take a while since it needs to download a lot, so grab some coffee or water while it finishes.  Fortunately for us, it will be much faster once all of the dependencies are downloaded.

Now run the command ```./run.sh```  Congratulations!  Visallo is running and you can work with the app.  If you have problems, make sure that your port 8080 is clear and try to run the script again.

Point your browser to localhost:8080 and Visallo will load the login page.  Use username: ```admin``` and password: ```admin``` to log in and you will be presented with your Visallo Dashboard.

## Working with the app

You should definitely spend some time looking around Visallo.  When you download the archetype and artifacts it comes prebundled with:

* An example authentication plugin that logs you in as long as your user name and password are the same
* An example graph property worker that extracts person names from a csv that is imported
* An web app plugin which adds the ability to google a person concept's name from the details pane of that entity inside of Visallo

Since we are already logged in as admin with the password of admin, we know that the authentication module works and the source code can be found inside of your project in the ```./auth``` folder.

### Example Graph Property Worker

The graph property worker that is inside of your project is inside of the ```./worker``` folder.  It is designed to pull names outside of csv files, add the entities inside as people, then show the results on your workspace. 

* Switch to the graph view by clicking the graph button on the left hand side of your screen.
* Drag and drop the file at ```./worker/src/test/resources/contacts.csv``` in your project onto the graph.
* Click Import on the dialog that pops up.

The file was imported into Visallo and run through the graph property work queue inside of your server.  It pulled out Bruce Wayne and Clark Kent from inside of the csv and added them to your graph.

### Example Web Plugin

Now that we have some people in our system, we can use the example web plugin to google their names.  To see the code that makes up this plugin, look inside of the ```./web``` folder in your project.  Click on "Bruce Wayne" in the graph and the details pane will open.  In the details pane you can view the information that you are allowed to see about Bruce Wayne.  Click the "Google" button at the top menu bar of the details pane to open a new window that automatically opens the search results page for "Bruce Wayne" in Google.  

## Now What?

In our next tutorial, we will work through Visallo's version of Hello World  by [creating a custom Hello World Graph Property Worker](helloworldgpw.md)
