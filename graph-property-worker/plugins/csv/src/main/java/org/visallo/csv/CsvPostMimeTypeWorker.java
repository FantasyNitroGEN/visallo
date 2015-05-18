package org.visallo.csv;

import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.PostMimeTypeWorker;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.vertexium.Authorizations;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.vertexium.mutation.ExistingElementMutation;

@Name("CSV")
@Description("Identifies CSV files and limits the GPW to only CsvGraphPropertyWorker")
public class CsvPostMimeTypeWorker extends PostMimeTypeWorker {
    private static final String MIME_TYPE_TEXT_CSV = "text/csv";
    private static final String MULTI_KEY = CsvPostMimeTypeWorker.class.getName();

    @Override
    public void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception {
        if (MIME_TYPE_TEXT_CSV.equals(mimeType)) {
            addCsvGraphPropertyWorkerToWhiteList((Vertex) data.getElement(), data.getVisibility(), authorizations);
        }
    }

    private void addCsvGraphPropertyWorkerToWhiteList(Vertex vertex, Visibility visibility, Authorizations authorizations) {
        ExistingElementMutation<Vertex> m = vertex.prepareMutation();
        VisalloProperties.GRAPH_PROPERTY_WORKER_WHITE_LIST.addPropertyValue(m, MULTI_KEY, CsvGraphPropertyWorker.class.getName(), visibility);
        m.save(authorizations);
    }
}
