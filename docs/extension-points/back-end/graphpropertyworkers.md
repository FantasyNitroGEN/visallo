# Graph Property Workers

## Overview

[Graph Property Workers](../../java/org/visallo/core/ingest/graphProperty/GraphPropertyWorker.html) are designed for data enhancement and individual scoring analytics on each element or property inside of Visallo.  Graph Property Workers will get notified of every change made to elements and properties in the system and allow other Graph Property Workers to act on those changes.  For example, the [PhoneNumberGraphPropertyWorker](../../java/org/visallo/phoneNumber/PhoneNumberGraphPropertyWorker.html) analyzes each property of every element in the system and tries to determine if there is a phone number in the text.  It then proposes that the phone number it found should be resolved to a concept that is defined in the ontology and broadcasts the changes to the UI.

The Graph Property Workers follow the [blackboard design pattern](https://en.wikipedia.org/wiki/Blackboard_%28design_pattern%29) model.  Each Graph Property Worker notifies the thread that is running it that it can work on an element and an optional property.  If the worker returns true from its [```isHandled```](../../java/org/visallo/core/ingest/graphProperty/GraphPropertyWorker.html#isHandled-org.vertexium.Element-org.vertexium.Property-) method is called, then that Graph Property Worker's ```execute``` method is called with additional data.  The Graph Property Worker is then able to contribute data or run operations on that specific element or property.

## Development

There are many examples of Graph Property Workers in the open source Visallo project. You can find some of them [here](https://github.com/v5analytics/visallo/search?q=%22extends+GraphPropertyWorker%22&type=Code).  

For a bare-bones Graph Property Worker you must implement two methods: the [```execute```](../../java/org/visallo/core/ingest/graphProperty/GraphPropertyWorker.html#execute-java.io.InputStream-org.visallo.core.ingest.graphProperty.GraphPropertyWorkData-) method and the [```isHandled```](../../java/org/visallo/core/ingest/graphProperty/GraphPropertyWorker.html#isHandled-org.vertexium.Element-org.vertexium.Property-) method.

### [```isHandled```](../../java/org/visallo/core/ingest/graphProperty/GraphPropertyWorker.html#isHandled-org.vertexium.Element-org.vertexium.Property-)
This method must return true if the specific GraphPropertyWorker can handle the element or property that is passed into that method.  Otherwise the method return false and the execute method on the GraphPropertyWorker will not be called.

### [```execute```](../../java/org/visallo/core/ingest/graphProperty/GraphPropertyWorker.html#execute-java.io.InputStream-org.visallo.core.ingest.graphProperty.GraphPropertyWorkData-)
All work should be done inside of this method for every Graph Property Worker.  The InputStream parameter is only populated if the value that is retrieved from the vertex is a StreamingPropertyValue.  The GraphPropertyWorkerData object that gets passed in is a data object that encapsulates all of the information about the element and the context in which the Graph Property Worker may need to consider.  See the [javadoc](../../java/org/visallo/core/ingest/graphProperty/GraphPropertyWorker.html) for more information.

## Use Cases

### Data Enhancement

Since Graph Property Workers look at the data on a per-element and per-property basis, data enhancement can be easily applied to each element. As an example, consider a video that is dragged onto the case by someone who is trying to discover if a specific person is in the video.  The following steps can happen if the correct Graph Property Workers are running and an appropriate ontology is loaded.

1. The file is uploaded to the server.
1. The server creates a vertex with the following properties set:
  * Raw property set to the bytes of the video file 
  * FileName property set to the name of the video file 
  * Concept type set to the raw concept
1. The vertex is queued and submitted to the Graph Property Workers.
1. Every graph property worker sees the new vertex after it is dequeued by the runner and the MimeTypeExtractor Graph Property Worker sees that it is a raw vertex, so its isHandled method returns true.
1. The MimeTypeExtractor Graph Property Worker sees that the raw bytes are a video and set the concept type to video.
1. After the MimeTypeExtractor is done, it requeues the vertex so that other Graph Property Workers can see the changes that were made.
1. Every Graph Property Worker sees the updated vertex from the queue and a Graph Property Worker splits the video into separate images and requeues the vertex.
1. Again, every graph property worker sees that video frames are now images on the vertex and a facial recognition Graph Property Worker pulls out the people in each image and checks to see if the specific person is in the video.

### Analytics

Imagine we wanted to update a fraud score on a person vertex. We could write a Graph Property Worker which listens for any changes made to any person vertex and update that fraud score. Below is some pseudo code on how you might do that.

```java
public class PersonFraudScoreGPW extends GraphPropertyWorker {
  public boolean isHandled(Element element, Property property) {
    return isPersonVertex(element);
  }

  public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
    double fraudScore = calculateFraudScore(data.getElement(), data.getElement().getEdges());
    data.getElement().setProperty("fraudScore", fraudScore);
  }
}
```

## Deployment

Graph Property Workers are deployed inside of the web server by default.  Having the Graph Property Workers inside of the web server works well for development and installations that do not need to scale up further than their web server.  On large installations, that may take too many resources from the web server so it may be required to move the Graph Property Workers out of the web server.  When each Graph Property Worker starts up, they are all started inside of their own threads and a GraphPropertyRunner coordinates each of them together.  It is possible to run more than one set of Graph Property Workers in the server which can be valuable if you are running on multi-core hardware.  To add more Graph Property Worker threads, ensure that the following is in your configuration:

```bash
#Set number of graph property worker threads to 4
org.visallo.web.initializers.GraphPropertyWorkerRunnerInitializer.threadcount=4
```

If you do not want the graph property workers running inside of the web server, add the following to your configuration.

```bash
#disable the graph property workers running inside of the web server
disable.org.visallo.web.initializers.GraphPropertyWorkerRunnerInitializer=true

```

## More Information

* [Graph Property Worker Tutorial](../../tutorials/helloworldgpw.md)
