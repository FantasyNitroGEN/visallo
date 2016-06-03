# Long Running Processes

## Overview

There are certain instances in which a task must be run in the background of Visallo.  Some typical reasons to run a background task in Visallo are:

* Incoming data needs to be run outside of the graph property framework
* Work done by a web request would take too long and would ruin the UX 
* You would like to spawn an asynchronous task a long-running task inside of Visallo that will notify the web UI when it is done but not block on a request

Visallo supports Long Running Processes in it's background.  Long Running Processes are similar to [Graph Property Workers](./graphpropertyworkers.md): they recieve a message from a queue, the Long Running Process decides if it can operate on the message, and then works on that message.  An example of a Long Running Process is [FindPathLongRunningProcessWorker](../../java/org/visallo/core/model/longRunningProcess/FindPathLongRunningProcessWorker.html).  When a find-path request is issued from the Graph on the UI, a web request is made to the server with the source and destination vertex and a message is queued into the configured long running process queue.  That message is then is dequeued by all of the current long running processes and the [FindPathLongRunningProcessWorker](../../java/org/visallo/core/model/longRunningProcess/FindPathLongRunningProcessWorker.html) sees that it can work on the message, finds all of the paths between the specified vertices, and sends those results to the UI.

## Development

Long Running processes have an [```isHandled```](../../java/org/visallo/core/model/longRunningProcess/LongRunningProcessWorker.html#isHandled-org.json.JSONObject-) method and a [```processInternal```](../../java/org/visallo/core/model/longRunningProcess/LongRunningProcessWorker.html#processInternal-org.json.JSONObject-) method that need to be implemented as a minimum to create a LongRunningProcessWorker.  Like the graph property workers, the [```isHandled``](../../java/org/visallo/core/model/longRunningProcess/LongRunningProcessWorker.html#isHandled-org.json.JSONObject-) method allows the long running process to filter through the messages that are put onto the long running process queue.  Then, the canonical class reference must be in that same project's org.visallo.core.model.longRunningProcess.LongRunningProcessWorker file so it can be loaded from the classpath.

### [```isHandled```](../../java/org/visallo/core/model/longRunningProcess/LongRunningProcessWorker.html#isHandled-org.json.JSONObject-) 
This method returns true if the Long Running Process Worker can operate on the incoming message.  Otherwise, it returns false.

### [```processInternal```](../../java/org/visallo/core/model/longRunningProcess/LongRunningProcessWorker.html#processInternal-org.json.JSONObject-) 
This method handles the message that the isHandled method above returned true for.

## Deployment

Long running processes are deployed inside of the webserver by default.  Running the Long Running Processes in the web server is good enough for a typical installation, but the requirements are going to vary widely depending on things like:

* your data size 
* how resource-intensive your long running processes are on the server
* the Vertexium plugins that are enabled
* the number of cores on each box

Long Running Processes are run inside of their own threads on the server and run independently of each other.  It is possible to run more than one instance of long running processes which can be useful if your Long Running Process message queue gets backed up. 

To add more instances of each Long Running Process Worker, simply add/change the following in your properties file:

```bash
#Set number of long running process threads to 2
org.visallo.web.initializers.LongRunningProcessRunnerInitializer.threadCount=2
```

If you do not want Long Running Processes to run inside of the web server, add the following to your configuration:

```bash
#disable the long running processes running inside of the web server
disable.org.visallo.web.initializers.LongRunningProcessRunnerInitializer=true
```

