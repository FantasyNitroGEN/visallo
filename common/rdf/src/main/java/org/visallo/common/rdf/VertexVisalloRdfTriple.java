package org.visallo.common.rdf;

import org.vertexium.Visibility;

public class VertexVisalloRdfTriple extends VisalloRdfTriple {
    private final String vertexId;
    private final Visibility vertexVisibility;
    private final String vertexVisibilitySource;

    protected VertexVisalloRdfTriple(String vertexId, Visibility vertexVisibility, String vertexVisibilitySource) {
        this.vertexId = vertexId;
        this.vertexVisibility = vertexVisibility;
        this.vertexVisibilitySource = vertexVisibilitySource;
    }

    public String getVertexId() {
        return vertexId;
    }

    public Visibility getVertexVisibility() {
        return vertexVisibility;
    }

    public String getVertexVisibilitySource() {
        return vertexVisibilitySource;
    }
}
