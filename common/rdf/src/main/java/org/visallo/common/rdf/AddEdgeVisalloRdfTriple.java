package org.visallo.common.rdf;

import com.google.common.base.Strings;
import org.vertexium.Authorizations;
import org.vertexium.mutation.ElementMutation;

public class AddEdgeVisalloRdfTriple extends VisalloRdfTriple {
    private final String edgeId;
    private final String edgeLabel;
    private final String outVertexId;
    private final String inVertexId;
    private final String edgeVisibilitySource;

    protected AddEdgeVisalloRdfTriple(
            String edgeId,
            String outVertexId,
            String inVertexId,
            String edgeLabel,
            String edgeVisibilitySource
    ) {
        this.edgeId = edgeId;
        this.edgeLabel = edgeLabel;
        this.outVertexId = outVertexId;
        this.inVertexId = inVertexId;
        this.edgeVisibilitySource = edgeVisibilitySource;
    }

    public String getEdgeId() {
        return edgeId;
    }

    @Override
    public String getElementId() {
        return getEdgeId();
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

    public String getEdgeVisibilitySource() {
        return edgeVisibilitySource;
    }

    @Override
    public String toString() {
        String label = getEdgeLabel();
        if (!Strings.isNullOrEmpty(getEdgeId())) {
            label += ":" + escape(getEdgeId(), ':');
        }
        if (!Strings.isNullOrEmpty(getEdgeVisibilitySource())) {
            label += String.format("[%s]", getEdgeVisibilitySource());
        }
        return String.format("<%s> <%s> <%s>", getOutVertexId(), label, getInVertexId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AddEdgeVisalloRdfTriple that = (AddEdgeVisalloRdfTriple) o;

        if (edgeId != null ? !edgeId.equals(that.edgeId) : that.edgeId != null) {
            return false;
        }
        if (edgeLabel != null ? !edgeLabel.equals(that.edgeLabel) : that.edgeLabel != null) {
            return false;
        }
        if (outVertexId != null ? !outVertexId.equals(that.outVertexId) : that.outVertexId != null) {
            return false;
        }
        if (inVertexId != null ? !inVertexId.equals(that.inVertexId) : that.inVertexId != null) {
            return false;
        }
        if (edgeVisibilitySource != null ? !edgeVisibilitySource.equals(that.edgeVisibilitySource) : that.edgeVisibilitySource != null) {
            return false;
        }

        return super.equals(that);
    }

    @Override
    public ImportContext updateImportContext(
            ImportContext ctx,
            RdfTripleImportHelper rdfTripleImportHelper,
            Authorizations authorizations
    ) {
        ElementMutation m = rdfTripleImportHelper.getGraph().prepareEdge(
                getEdgeId(),
                getOutVertexId(),
                getInVertexId(),
                getEdgeLabel(),
                rdfTripleImportHelper.getVisibility(getEdgeVisibilitySource())
        );
        return new ImportContext(getEdgeId(), m);
    }

    @Override
    public int hashCode() {
        return edgeId.hashCode();
    }
}
