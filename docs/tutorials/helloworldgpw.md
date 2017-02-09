# Your First Graph Property Worker

## Prerequisites

Ensure that you have gone through the [starting](starting.md) instructions.  There will be some development in this tutorial so we recommend that you at least be familiar with the command line, maven and a java IDE that works with maven (e.g. Intellij).  

## Background

One of the major parts of Visallo is the system of Graph Property Workers that enhance and analyze the data.  Since most organizations are going to have different use-cases and needs for working with the data, we designed the graph property workers we designed to be as pluggable as possible.  For more information on graph property workers, please [read the documentation](../extension-points/back-end/graphpropertyworkers.md) about graph property workers before beginning this tutorial.

[After the last section](starting.md), we have created our own visallo project to run Visallo, but haven't customized it.  We will be working in the same directory to add our own functionality to that app.

## Graph Property Worker Skeleton

With the web app loaded, (```mvn clean package && ./run.sh``` in the project directory like [in the starting tutorial](starting.md))  click on the admin menu item in the left hand menu bar and click on Plugin > List to see the loaded plugins.  Expand the Graph Property Workers tab and take a look at the loaded graph property workers.  We don't see our Hello World Graph Property Worker but we do see an example Graph Property Worker that was brought in when we pulled down our maven archetypes.  We will need to create our graph property worker and put it on the classpath.

<i>At this time I recommend opening your project in an IDE, like Intellij, since we are about to write some code.</i>

Stop the server by hitting Ctrl+c where maven was running the project from.

Navigate into the ```./worker/src/main/java/com/visalloexample/worker``` directory and create a new class called HelloWorldGraphPropertyWorker.  Make this class extend GraphPropertyWorker and create the two required abstract methods isHandled and execute.  The file should look like the following:


```java
package com.visalloexample.helloworld.worker;

import org.vertexium.Element;
import org.vertexium.Property;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;

import java.io.InputStream;

public class HelloWorldGraphPropertyWorker extends GraphPropertyWorker {
    @Override
    public boolean isHandled(Element element, Property property) {
        return false;
    }

    @Override
    public void execute(InputStream inputStream, GraphPropertyWorkData graphPropertyWorkData) throws Exception {

    }
}
```

This is the most barebones that a graph property worker can be.  In order to load it on the classpath, we need to modify the services file in the resources directory for java.  Go into the ```worker/src/main/resources/META-INF/services``` directory and add the HelloWorldGraphPropertyWorker line to the file ```org.visallo.core.ingest.graphProperty.GraphPropertyWorker```.  It should now look like this:

```bash
com.visalloexample.helloworld.worker.ExampleGraphPropertyWorker
com.visalloexample.helloworld.worker.HelloWorldGraphPropertyWorker
```

Go back to the root of your project and run ```mvn clean package```, then ```./run.sh``` and wait for the server to come up.  Go back to the admin pane, check the list of plugins, and expand the graph property worker drop down.  You will now see your graph property worker in the list of plugins.  Unfortunately, there isn't a nice name for it or a description, so let us add one.  
Add the following annotations to the class (look at the example graph property worker for a reference)

```java
@Name("My Hello World Graph Property Worker")
@Description("Sets the title of every person vertex to Hello World")
```

Run ```mvn clean package```, then ```./run.sh``` in your project again.  You will see the name and description update in the admin plugin list.

## Adding Functionality

Right now we have a skeleton of a graph property worker, but it doesn't do anything.  As it stands, it only tells the entire graph property framework that it can't work on anything because it returns false every time the isHandled method is called.  We want to enable it to do whatever it needs to do.  Ctrl+C out of the web server then change the isHandled method to return element instanceof Vertex and import the correct classes.  

Now your code should look like:

```java
// ... imports omitted
@Name("My Hello World Graph Property Worker")
@Description("Sets the title of every person vertex to Hello World")
public class HelloWorldGraphPropertyWorker extends GraphPropertyWorker {
    @Override
    public boolean isHandled(Element element, Property property) {
        return element instanceof Vertex;
    }

    @Override
    public void execute(InputStream inputStream, GraphPropertyWorkData graphPropertyWorkData) throws Exception {

    }
}
```

But that isn't all.  We only want to deal with people vertices, not every vertex is going to have a full name property.  To do that, we are going to need put one more boolean statement inside of the isHandled method.

```java
"http://example.org/visallo-helloworld#person".equals(VisalloProperties.CONCEPT_TYPE.getPropertyValue(element));
```

Your HelloWorldGraphPropertyWorker should now look like this:

```java
@Name("My Hello World Graph Property Worker")
@Description("Sets the title of every person vertex to Hello World")
public class HelloWorldGraphPropertyWorker extends GraphPropertyWorker {
    @Override
    public boolean isHandled(Element element, Property property) {
        return element instanceof Vertex && "http://example.org/visallo-helloworld#person".equals(VisalloProperties.CONCEPT_TYPE.getPropertyValue(element));
    }

    @Override
    public void execute(InputStream inputStream, GraphPropertyWorkData graphPropertyWorkData) throws Exception {
    }
}
```

Now we will get only the person vertices in the execute method, but we still aren't doing anything with them.  Add the following lines to the execute method to actually do the grunt work of setting the person vertices names to Hello World.  

```java
// gets the vertex from the data object that is passed in
Vertex v = (Vertex)graphPropertyWorkData.getElement();

// sets the property on the vertex, using the visibility of the vertex and the authorizations of the graph property worker
v.setProperty("http://example.org/visallo-helloworld#fullName", "Hello World", v.getVisibility(), getAuthorizations());

// flush the changes to the graph
getGraph().flush();

// notify the UI and future workers that there was a change to the data
getWorkQueueRepository().pushGraphPropertyQueue(v, "", "http://example.org/visallo-helloworld#fullName", Priority.NORMAL);
```

Since we now are changing the properties, we need to make sure that the graph property worker won't change the title to "Hello World" continually.  Add the following boolean condition to the isHandled method

```java
!"Hello World".equals(element.getPropertyValue("http://example.org/visallo-helloworld-gpw#fullName"))
```

Your HelloWorldGraphPropertyWorker class now looks like:

```java
@Name("My Hello World Graph Property Worker")
@Description("Sets the title of every person vertex to Hello World")
public class HelloWorldGraphPropertyWorker extends GraphPropertyWorker {
    @Override
    public boolean isHandled(Element element, Property property) {
        return element instanceof Vertex &&
               "http://example.org/visallo-helloworld#person".equals(VisalloProperties.CONCEPT_TYPE.getPropertyValue(element)) &&
               !"Hello World".equals(element.getPropertyValue("http://example.org/visallo-helloworld-gpw#fullName"));
    }

    @Override
    public void execute(InputStream inputStream, GraphPropertyWorkData graphPropertyWorkData) throws Exception {
        Vertex v = (Vertex)graphPropertyWorkData.getElement();
        v.setProperty("http://example.org/visallo-helloworld#fullName", "Hello World", v.getVisibility(), getAuthorizations());
        getGraph().flush();
        getWorkQueueRepository().pushGraphPropertyQueue(v, "", "http://example.org/visallo-helloworld#fullName", Priority.NORMAL);
    }
}
```

Go to your project root and run ```mvn clean package``` and then ```./run.sh``` again to bring the web app back up.

We need to test out that the graph property worker actually makes the changes that we intend.  To do that, we are going to add an entity to the graph and watch it's name change.

1. Click on Graph in the right hand side of the screen 
1. Right click on the graph.
1. Click New Entity... 
1. In the concept type text box, type Person and hit enter
1. Click on Create

You will see a person get added to the graph, and their name quickly change from "No Title Available" to "Hello World".

[Next Steps: Making a Web Plugin](webplugin.md)
