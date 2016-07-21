# External Resource Workers

## Overview

Visallo can ingest any kind of data but sometimes that ingestion process is not as straightforward as dragging data onto Visallo's UI or creating a simple connector to hit a rest API endpoint.  External resource workers run tasks asynchronously using Visallo's resources so that more complicated tasks or tasks that take a long time to complete will not interfere with other parts of the architecture.

Consider the following example:

You want to ingest all of the data from a specific rest end point into Visallo, but you are rate-limited by those API calls.  It is possible to write a command line tool to handle the data import, but you want a managed solution that works while Visallo is running and can be monitored from the Visallo UI.

In that instance, you may want to consider an external resource worker.  It can automatically hit the API end-point, run as fast as the rate limit is, and then pause execution as is needed.

## Development

There are only two methods that you must implement when creating an [external resource worker](../../java/org/visallo/core/externalResource/ExternalResourceWorker.html), [```run```](../../java/org/visallo/core/externalResource/ExternalResourceWorker.html#run--) and [```stop```](../../java/org/visallo/core/externalResource/ExternalResourceWorker.html#stop--).  The abstraction over these external process workers are extremely simple to support many possible use cases.  A provided helper class called [```org.visallo.core.externalResource.QueueExternalResourceWorker```](../../java/org/visallo/core/externalResource/QueueExternalResourceWorker.html) will support some default functionality that will dequeue data from a queue and process it. 

## Deployment

External resource workers are run inside the web server when it is brought online.  They are injected through the service loader and then their lifecycle is started when their start method is called.  To ensure that an ExternalResourceWorker is loaded on the classpath, it must be on the classpath and be referenced in a file org.visallo.core.externalResource.ExternalResourceWorker in the META-INF/services folder in the jar. 

If you do not want to run ExternalResourceWorkers inside of the server, add the following to your configuration.

```bash
#disable the External Resource Workers from running inside of the web server
disable.org.visallo.web.initializers.ExternalResourceWorkersInitializer=true
```
