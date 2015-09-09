# Architecture Overview

While Visallo leverages a lot of great open source software, its own code is consolidated around two components: the
ingestion pipeline and the web application. The sections below describe all of this in more detail.

## Software Stack

The diagram below shows the major software components used by Visallo. Visallo-specific code is written against many of
these layers, but is not shown in the diagram. There are numerous other libraries not listed. Take a look at the parent
[POM file](../pom.xml) or run `mvn dependency:tree` to get a feel for all the libraries involved.

![Visallo Software Stack](img/visallo-software-stack.png)

## Security

Visallo implements a security mechanism known as cell-level security. Every vertex, property, and relationship has its own security label stored in both Accumulo and Elasticsearch, which is used to determine whether a given user meets the security requirements to view the value. This enables data of various security levels to be stored within the same vertex or edge and users of varying degrees of access to query the same graph, while preserving data confidentiality.

## External Integration

Visallo provides a REST API to access and manipulate the graph while keeping your data secure. The REST API can be secured using a number of pluggable authentication and authorization providers. If your enterprise needs low-level access, Vertexium can also be used to integrate Visallo graph data into other applications and services.

## Extensibility

Visallo has many extension points, from the underlying data store to data processing algorithms to UI plugins. As data is added to the system these extension points are used to analyze and transform the data into usable intelligence. Visallo Enterprise relies on the open extension points provided by Visallo open source to enhance your experience in meaningful and actionable ways, giving you more insight into your data.

## Ingestion Pipeline

The ingestion pipeline is primarily implemented using Apache YARN, although you can also ingest data using map reduce.
Instances of both approaches can be found in the Visallo code base.

### YARN-based Ingestion

YARN-based ingestion within Visallo works on top of an abstraction called `GraphPropertyWorker`(s). The abstraction
works a lot like data binding within GUI programming toolkits, with changes to properties of vertices in the graph
being published to a queue. All `GraphPropertyWorker`s are then given an opportunity to inspect the property change and
optionally perform some kind of action, possibly publishing new property changes to the queue for other workers to
process further.

Visallo-provided `GraphPropertyWorker` implementations are found in the `visallo-graph-property-worker-plugins-group` module.
Each is implemented as a plugin and will automatically run if found in the classpath. Please see
the [yarn features](features.md) page for more information about each plugin.

### Map Reduce Ingestion

Map reduce is a more appropriate programming model for batch ingestion of large datasets. Visallo doesn't currently
provide any batch ingestion abstraction similar to what's provided for yarn-based ingestion. You can see several map
reduce ingestion examples in the datasets directory. The most important thing to remember about those examples
is that they require running a re-indexing map reduce job after ingestion.

## Web Application

The Visallo web application backend is written in Java while the front-end is JavaScript and CSS, basically the same
technologies used by many other web applications. It's implemented as a single-page web application utilizing both
AJAX calls and persistent web-socket connections (using Atmosphere) to exchange data.
