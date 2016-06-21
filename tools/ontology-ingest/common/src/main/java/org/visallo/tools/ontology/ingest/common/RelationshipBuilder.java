package org.visallo.tools.ontology.ingest.common;

public abstract class RelationshipBuilder extends EntityBuilder {
    private String inVertexId;
    private String inVertexIri;
    private String outVertexId;
    private String outVertexIri;

    public RelationshipBuilder(String id, String inVertexId, String inVertexIri, String outVertexId, String outVertexIri) {
        super(id);
        this.inVertexId = inVertexId;
        this.inVertexIri = inVertexIri;
        this.outVertexId = outVertexId;
        this.outVertexIri = outVertexIri;
    }

    public String getInVertexId() {
        return inVertexId;
    }

    public String getInVertexIri() {
        return inVertexIri;
    }

    public String getOutVertexId() {
        return outVertexId;
    }

    public String getOutVertexIri() {
        return outVertexIri;
    }
}
