# Making a web plugin

## Introduction

If you went through the [last tutorial](./helloworldgpw.md), you already have your own customized graph property worker that will change the titles of every new entity on the graph to "Hello World".  Continuing with the hello world theme, we want to make sure that we can communicate between both our front end and our back end.  To discover how to add an item to the menu bar of the details pane, we recommend taking a look at the web plugin example that is already loaded in your ```web``` folder.  For this tutorial, we will be adding an item to the right-click context menu so that we can send information about the vertex back to the server.

## Scaffolding the project

Lets do the scaffolding of creating a new web app plugin so that we can delve into more detail on how the web app plugins are designed once it is done.  We will be working in the ```web``` folder in our project, so create a java file at ```web/src/main/java/com/visalloexample/helloworld/web/SelectedVertexWebAppPlugin.java``` and put the following into that file:

```java
package com.visalloexample.helloworld.web;

import com.v5analytics.webster.Handler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Selected Vertex Action Web App Plugin")
@Description("Registers a new menu item which will send back the vertex that it was clicked from and call some action on it")
public class SelectedVertexWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {

    }
}  
```

This class is responsible for telling the web server exactly which files it will be serving and in which context it will be serving them.  Next, we need to load our web plugin into the web server using service loading, so open up the ```web/src/main/resources/META-INF/services/org.visallo.web.WebAppPlugin``` and add the line

```
com.visalloexample.helloworld.web.SelectedVertexWebAppPlugin
```

so now that file is going to look like:

```properties
com.visalloexample.helloworld.web.ExampleWebAppPlugin
com.visalloexample.helloworld.web.SelectedVertexWebAppPlugin
```

Lets just make sure that the web app plugin appears in the application.  At the root of your repository, run ```mvn clean package``` and then run ```./run.sh```.  When the application comes up, enter the application using admin/admin and look at the list of plugins in the admin pane.  Under the webapp directory, you should see that you have an entry for the web plugin that you just made.  Congratulations!  You successfully made a web plugin, but of course, it doesn't do anything.

## Making the web plugin do something

There are two possible parts to each web plugin, the back-end code which will handle any web requests and the front-end code which will add to Visallo's UI.  We will be adding a little bit of both.  To start, let's create the menu item on the front-end.

To start, we need to create the files that will be served by Visallo and then tell the web server to serve them up.  Create a file ```web/src/main/resources/com/visalloexample/helloworld/web/selectedvertexplugin.js``` and add the following as contents of the file.

```javascript
require([
    'public/v1/api'
], function (
    api
) {
    'use strict';

    //register the extension to the registry.  Visallo will query the registry to know what plugins to run.
    //This code will create a new menu in the context menu and, when clicked, will launch the 'doAction' event through the dom thanks to flight.
    api.registry.registerExtension('org.visallo.vertex.menu', {
        label: 'Do Action',
        event: 'doAction'
    });
    
    //create an handle for the doAction event that gets created from above.
    $(document).on('doAction', function(e, data) {
        //output the fact that there was an event and the parameters to the console.
        console.log("action happened", e, data);
    });
});
```

We are almost done.  Now, we need to tell the web server that we want this javascript to be served on every page.  Open up the ```web/src/main/java/com/visalloexample/helloworld/web/SelectedVertexWebAppPlugin.java``` and add the following line into the init method.

```java
app.registerJavaScript("/com/visalloexample/helloworld/web/selectedvertexplugin.js", true);
```

Now we are telling Visallo where to load the javascript from.  It will be on the classpath since the javascript is bundled in with the war which is why the location looks strange.  By setting the last boolean parameter to true, we are telling it that we want that javascript included on the page when the page loads.  Loading this file on the page is the correct thing to do when the page loads so that our plugin can be registered, but you don't typically want too many things to be loaded on the page because it will make startup slower.  Try to defer loading of resources as long as possible.

Now your web plugin class should like the following:

```java
package com.visalloexample.helloworld.web;

import com.v5analytics.webster.Handler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Selected Vertex Action Web App Plugin")
@Description("Registers a new menu item which will send back the vertex that it was clicked from and call some action on it")
public class SelectedVertexWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        app.registerJavaScript("/com/visalloexample/helloworld/web/selectedvertexplugin.js", true);
    }
}
```

Go to the root of your project and run ```mvn clean package && ./run.sh``` again and reload the web browser to see the changes that we made.  Create a vertex and right click on it; you should see a menu item that says 'Do Action' inside of it.  To have it fire an event, catch the event, and output to the console: make sure your javascript console is open and then simply click on the menu item.  You will see a message that is output to the console corresponding to what we have in the plugin.  

Congratulations!  We now have a front-end-only web plugin which is pretty good.  But what we really want to do is be able to communicate from the front-end to the back-end and do something with a repsonse on the front-end.  In our next steps we will create and use a web-worker to send an ajax request back to the server, do something with it, and receive some data on the front end.

###  Communicate to the back end through a web worker

To start, let's create an endpoint that we can have the ajax request hit.  For right now, we are just going to ```System.out.println``` the information that we get receive from the front end.

To do that, we need to register an endpoint to let the front end hit it, so lets create the callback first.  Create a class called SelectedVertexAction at ```web/src/main/java/com/visalloexample/helloworld/web/SelectedVertexAction.java``` with the following contents:

```java

package com.visalloexample.helloworld.web;

import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.clientapi.model.ClientApiSuccess;

public class SelectedVertexAction implements ParameterizedHandler {
    @Handle
    public ClientApiObject handle(
            @Required(name = "vertexId") String vertexId
    ) {
        System.out.printf("Received a post with %s as the specified vertexId\n", vertexId);
        return new ClientApiSuccess();
    }
}
```

This class is pretty simple, it will output the vertex that was clicked on and return that it was successful.  Next, we are going to need to register this class with Visallo so open up the SelectedVertexWebAppPlugin class and add the following line: 

```java
   app.post("/selected", authenticationHandler.getClass(), VisalloCsrfHandler.class, SelectedVertexAction.class);
``` 

into the ```init``` method.  There are a couple of parameters that you don't have to worry about right now that are a part of the method call, but the most important paramter is the ```SelectedVertexAction.class``` parameter which references the class which will handle the web request.

Unfortunately we aren't done here.  We need to write the web worker that will actually call that endpoint.  Create a file ```web/src/main/resources/com/visalloexample/helloworld/web/selectedvertexwebworker.js``` and add the following code: 

```javascript
define('data/web-worker/services/selectedvertex', [
    'public/api/workerApi'
], function(workerApi) {
    var ajax = workerApi.ajax;
    'use strict';
    return {
        selected: function(vertexId) {
            return ajax('POST', '/selected', { vertexId: vertexId });
        }
    }
})
```

This web worker is the data layer between the rest api and the javascript code.  It will run inside of a promise that will allow the code that calls it to use the information that is returned by the ajax request.  Since we are currently returning the ClientApiSuccess, it will only return a true value wrapped in a json object, but it will become more important to pass the data back when we are doing more complicated calculations on the server.

Only one more thing to do, let the javascript use the web worker to make the callback.  Change the code in ```web/src/main/resources/com/visalloexample/helloworld/web/selectedvertexplugin.js``` to look like the following:

```javascript 
require([
    'public/v1/api'
], function (
    api
) {
    'use strict';

    api.registry.registerExtension('org.visallo.vertex.menu', {
        label: 'Do Action',
        event: 'doAction'
    });

    $(document).on('doAction', function(e, data) {
        //require the data object necessary to do the ajax request
        api.connect().then(function(connectApi) {

            //do the ajax request.  Notice how 'selectedvertex' is defined at the top of the web worker and 'selected' is the method name in the web worker
            connectApi
                .dataRequest('selectedvertex', 'selected', data.vertexId)
                .then(function(response) {
                    //just console.log the result
                    console.log("got response back from the server!", response);
                });
        });
    });
});
```

Save that file and redo the ```mvn clean package && ./run.sh``` in your project.  Right click on a vertex in the graph and click on the 'Do Action' menu.  On the server, you will now see a message that looks like "Received a post with b5ff78be67b7409e8537edf01e0119e9 as the specified vertexId" on the server and in your javascript console inside of your browser, you will see a message that looks like got data back from the server! Object {success: true}

### Doing something with the data

Typically, you are going to want to do something on the back end, and then send the results to the front.  To demonstrate this concept, we will need to make a couple of changes to what happens in the route that we just made.  Change the ```SelectedVertexAction.java``` file to look like the following: 

```java
package com.visalloexample.helloworld.web;

import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SelectedVertexAction implements ParameterizedHandler {
    private Graph graph;
    private WorkQueueRepository repository;

    @Inject
    public SelectedVertexAction(Graph graph, WorkQueueRepository repository) {
        this.graph = graph;
        this.repository = repository;
    }

    @Handle
    public ClientApiObject handle(
            @Required(name = "vertexId") String vertexId,
            @ActiveWorkspaceId(required = false) String workspaceId,
            Authorizations authorizations
    ) {
        //get the vertex that was sent back from the front end
        Vertex v = graph.getVertex(vertexId, authorizations);

        //put the current date and time into the string
        String format = new SimpleDateFormat().format(new Date());

        //make a new title for the vertex
        final String newName = String.format("Action (%s)", format);

        //set the property on the vertex
        v.setProperty("http://example.org/visallo-helloworld-gpw#fullName", newName, v.getVisibility(), authorizations);

        // make sure that the changes are persisted into the graph
        this.graph.flush();

        //tell the workspace that the vertex has changed so it needs to be reloaded
        this.repository.broadcastElement(v, workspaceId);

        return new ClientApiSuccess();
    }
}
```

This code will now change the name of the vertex that the menu item was opened on to "Action (<the current time>)".  Note that, in this example, we didn't send the information back to the front-end through the web request.  We instead broadcasted a message to the front end that told it that the element that we clicked on had changed, and that it needed to reload it.  

Let's send the new title to the front end and have it create a simple javascript alert box.  At the end of the ```handle``` method, instead of returning the ClientApiSuccess object return: 

```java
        return new ClientApiObject() {
            public String getNewName(){
                return newName;
            }
        };
```

Now we are passing the message back to the front end instead of just passing back a success message.  Go into the ```selectedvertexplugin.js``` file and add the following instead of the console message:

```javascript
  alert("Vertex " + data + " title changed to " + response.title);
```

The new name of the element will be passed back to the front end and we can construct a javascript alert with it in there.

### Conclusion

So that was a tutorial that took you through the basic steps of creating a webapp plugin.  We created a web plugin that can pass data from the front end to the back end, and then send data in multiple ways back to the front end.  Using these simple concepts, it is possible to build some more of the complicated behaviors that makes Visallo customized for your organization.  
