package org.visallo.common.rdf;

import org.vertexium.Visibility;

public class AddEdgeVisalloRdfTriple extends VisalloRdfTriple {
    private final String edgeId;
    private final String edgeLabel;
    private final String outVertexId;
    private final String inVertexId;
    private final Visibility edgeVisibility;

    protected AddEdgeVisalloRdfTriple(
            String edgeId,
            String outVertexId,
            String inVertexId,
            String edgeLabel,
            Visibility edgeVisibility
    ) {
        this.edgeId = edgeId;
        this.edgeLabel = edgeLabel;
        this.outVertexId = outVertexId;
        this.inVertexId = inVertexId;
        this.edgeVisibility = edgeVisibility;
    }

    public String getEdgeId() {
        return edgeId;
    }

    public String getEdgeLabel() {
        return edgeLabel;
    }

    public String getOutVertexId() {
        return outVertexId;
    }

    public String getInVertexId() {
        return inVertexId;
    }

    public Visibility getEdgeVisibility() {
        return edgeVisibility;
    }
}
