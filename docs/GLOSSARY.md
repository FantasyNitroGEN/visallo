## vertex
A node in the graph that can have properties and edges to other vertices.

## edge
A connection between two vertices in the graph.

## ontology
The valid concepts, properties, and relationships for a Visallo installation.

## concept
A type defined in the ontology (e.g. person, place, or company). Uniquely identified by an IRI and
assigned to every vertex in the graph.

## IRI
Internationalized Resource Identifier. e.g. http://visallo.org#person or http://visallo.org#worksFor

## property
A field defined in the ontology as valid for one or more concepts. Uniquely identified by an IRI
and optionally set on vertices in the graph.

## relationship
A connection defined in the ontology as valid from one concept to another. Uniquely identified by
an IRI and stored as edges between vertices in the graph.

## owl
Web Ontology Language. Standard XML file format for defining ontologies.

## GPW
Acronym for graph property worker.

## graph property worker
Type of Visallo plugin that responds to changes in the graph and often used for data enrichment and
analytics. GPWs can respond to creation or update events on vertices, properties, or edges.

## raw
The property on a vertex used to store any imported data.

## visibility
The data access control applied to vertices, properties, and edges. The term 'visibility' is borrowed from Accumulo.

## visibility source
The data stored on behalf of the visibility user interface component to support displaying and editing
data access control settings. This data is converted by the configured
org.visallo.core.security.VisibilityTranslator to the org.visallo.core.security.VisalloVisibility
value used to enforce data access control.

## visibility json
A JSONObject consisting of the visibility source and a list of workspace ids. This value is stored
as metadata on all vertices, properties, and edges to support data access control.

## authorization
The data access control rights granted to Visallo users to control their access to vertices,
properties, and edges. The term 'authorization' is borrowed from Accumulo.

## workspace
A named collection of vertices that can be shared for collaboration with
other Visallo users. New and changed vertices, properties, and edges
are only visible within a workspace until being published by a user with
the PUBLISH privilege.
