
# Vertexium Ingest

Visallo uses Vertexium as a datastore so if you want data to appear in Visallo, at a very basic level you need to add data to vertexium and the data will be ready for Visallo to use. Visallo's data model is slightly more complicated than what was just described but that is the basic theory. The advantage of using Vertexium to ingest data into Visallo is that you will have access to the data at a lower level than Visallo works with the data so you will have full control over your ingestion pipeline.

## Creating a vertex

To create a vertex in Vertexium all you need to do is call `graph.addVertex`, pass the required parameters in, and call `graph.flush().` While it is true that will create a vertex, it is also necessary for you to set a couple of things on each vertex before creating it so that Visallo an start to use it.

```java
   //you will need to get a handle on the vertxium graph. If you run in the context of org.visallo.core.cmdline.CommandLineTool you can get the graph in this manner
   Graph graph = this.getGraph();

   Visibility visibility = Visibility.EMPTY;
   Date now = new Date();
   String modifiedByValue = "Command line Tool";

   VertexBuilder newvertex = graph.prepareVertex("newemailvertex", visibility);
   
   //set the concept type on the vertex
   VisalloProperties.CONCEPT_TYPE.setProperty(newvertex, EMAIL_CONCEPT, visibility);
   //setting the title
   newvertex.setProperty(TITLE_IRI, "vertexiumingestemail@visallo.com", visibility);

   //set modified by property to show who the vertex was modified by
   VisalloProperties.MODIFIED_BY.setProperty(newvertex, modifiedByValue, visibility);

   //set modified date on vertex to now
   VisalloProperties.MODIFIED_DATE.setProperty(newvertex, now, visibility);

   newvertex.save(graph.createAuthorizations());

   graph.flush();
```

Once this code is run, it will insert an email entity into the system. You will then be able to query this entity from the UI. If you would like to specify the visibility that the vertex + properties are saved at, you can create your own custom visibilities by instantiating a `new Visibility("myvisibility")` instead of using the `Visibility.EMPTY` visibility used in the previous code examples.

## Adding a property to a vertex

Once you have added the initial vertex with the few specific Visallo properties, the api is going to be very similar to that of vanilla Vertexium. You can add any property that you would like but it will only be displayed in Visallo if the matching property IRI is also in the ontology.

You have two options of adding a property to a vertex. For a one-off change you can simply query for the vertex, set the property and save it again.

```java
   Vertex newemailvertex = graph.getVertex("newemailvertex", graph.createAuthorizations());
   Metadata metadata = new Metadata();
   VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, new VisibilityJson(), visibility);
   newemailvertex.setProperty("http://visallo.org#source", "Changed Property", m, visibility, graph.createAuthorizations());
   ExistingElementMutation<Vertex> vertexExistingElementMutation = newemailvertex.prepareMutation();
```

If speed is a consideration or multiple updates will be happening, it is recommended to get an ElementBuilder in order add the property:

```java
   Vertex newemailvertex = graph.getVertex("newemailvertex", graph.createAuthorizations());
   ExistingElementMutation<Vertex> vertexExistingElementMutation = newemailvertex.prepareMutation();
   for(int i = 0; i < 10; i++) {
       Metadata metadata = new Metadata();
       VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, new VisibilityJson(), visibility);
        vertexExistingElementMutation.addPropertyValue(String.format("source%d", i), "http://visallo.org#source", String.format("Property Mutation Update #%d", i), m, visibility);
    }

    vertexExistingElementMutation.save(graph.createAuthorizations());

    graph.flush();
```


## Creating an edge

The simplest way of creating an edge requires knowing the ids of two vertices that you would like to create an edge between. To create an edge:

```java
        createVertex("email1", EMAIL_CONCEPT, "email1@visallo.com");
        createVertex("email2", EMAIL_CONCEPT, "email2@visallo.com");

        Visibility visibility = Visibility.EMPTY;
        Graph graph = this.getGraph();
        graph.addEdge("hasContactedEdgeId", "email1", "email2", "http://visallo.org/sample#hasContacted", visibility, graph.createAuthorizations());
        graph.flush();

``` 
Ensure that the label matches an ObjectProperty IRI in your ontology or Visallo will not show the edge on its UI. Consult vertexium documentation or javadocs to discover different ways to create edges. 

## Adding an edge property

Adding edges to already existing edges is very similar to adding properties to vertices. To do one-off adding a property to an edge

```java
  Metadata m = new Metadata();
  VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(m, new VisibilityJson(), visibility);

  Edge hascontactedEdge = graph.getEdge("hasContactedEdgeId", graph.createAuthorizations());
  hascontactedEdge.setProperty("http://visallo.org/sample#about", "birthday party", m, visibility, graph.createAuthorizations());

  graph.flush();
```

If you are doing bulk inserts of edge properties you may want to consider using the following approach:

```java
   Metadata m = new Metadata();
   VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(m, new VisibilityJson(), visibility);

   Edge hascontactedEdge = graph.getEdge("hasContactedEdgeId", graph.createAuthorizations());
   ExistingEdgeMutation existingEdgeMutation = hascontactedEdge.prepareMutation();

   for(int i = 0; i < 10; i++) {
       existingEdgeMutation.addPropertyValue(String.format("about%d", i), "http://visallo.org/sample#about", String.format("birthday party for a 5%d year old", i), m, visibility);
   }

   existingEdgeMutation.save(graph.createAuthorizations());

   graph.flush();
```


## Further Reading

Start with the [tutorials](../../../tutorials/index.md) for hands on experience when ingesting
