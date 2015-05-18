package org.visallo.rdf;

import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.PostMimeTypeWorker;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.vertexium.Authorizations;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.vertexium.mutation.ExistingElementMutation;

@Name("RDF")
@Description("Limits RDF files to only be processed by RdfGraphPropertyWorker")
public class RdfPostMimeTypeWorker extends PostMimeTypeWorker {
    private static final String MULTI_KEY = RdfPostMimeTypeWorker.class.getName();

    @Override
    public void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception {
        if (RdfOntology.MIME_TYPE_TEXT_RDF.equals(mimeType)) {
            addRdfGraphPropertyWorkerToWhiteList((Vertex) data.getElement(), data.getVisibility(), authorizations);
        }
    }

    private void addRdfGraphPropertyWorkerToWhiteList(Vertex vertex, Visibility visibility, Authorizations authorizations) {
        ExistingElementMutation<Vertex> m = vertex.prepareMutation();
        VisalloProperties.GRAPH_PROPERTY_WORKER_WHITE_LIST.addPropertyValue(m, MULTI_KEY, RdfGraphPropertyWorker.class.getName(), visibility);
        m.save(authorizations);
    }
}
