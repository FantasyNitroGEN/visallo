package org.visallo.themoviedb;

import org.visallo.core.exception.VisalloException;
import org.visallo.vertexium.mapreduce.VisalloElementMapperBase;
import org.visallo.core.model.properties.VisalloProperties;
import org.apache.hadoop.io.BytesWritable;
import org.vertexium.Vertex;
import org.vertexium.VertexBuilder;
import org.vertexium.Visibility;
import org.vertexium.accumulo.AccumuloAuthorizations;
import org.vertexium.property.StreamingPropertyValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ImportImgMRMapper extends VisalloElementMapperBase<SequenceFileKey, BytesWritable> {
    public static final String MULTI_VALUE_KEY = ImportJsonMR.class.getName();
    public static final String SOURCE = "TheMovieDb.org";
    private Visibility visibility;
    private AccumuloAuthorizations authorizations;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        this.visibility = new Visibility("");
        this.authorizations = new AccumuloAuthorizations();
    }

    @Override
    protected void safeMap(SequenceFileKey key, BytesWritable value, Context context) throws Exception {
        String conceptType;
        String sourceVertexId;
        String edgeLabel;

        context.setStatus(key.getRecordType() + ":" + key.getId());

        switch (key.getRecordType()) {
            case PERSON:
                conceptType = TheMovieDbOntology.CONCEPT_TYPE_PROFILE_IMAGE;
                edgeLabel = TheMovieDbOntology.EDGE_LABEL_HAS_PROFILE_IMAGE;
                sourceVertexId = TheMovieDbOntology.getPersonVertexId(key.getId());
                break;
            case MOVIE:
                conceptType = TheMovieDbOntology.CONCEPT_TYPE_POSTER_IMAGE;
                edgeLabel = TheMovieDbOntology.EDGE_LABEL_HAS_POSTER_IMAGE;
                sourceVertexId = TheMovieDbOntology.getMovieVertexId(key.getId());
                break;
            case PRODUCTION_COMPANY:
                conceptType = TheMovieDbOntology.CONCEPT_TYPE_LOGO;
                edgeLabel = TheMovieDbOntology.EDGE_LABEL_HAS_LOGO;
                sourceVertexId = TheMovieDbOntology.getProductionCompanyVertexId(key.getId());
                break;
            default:
                throw new VisalloException("Invalid record type: " + key.getRecordType());
        }

        String edgeId = TheMovieDbOntology.getHasImageEdgeId(key.getId(), key.getImagePath());
        String title = key.getTitle();
        String vertexId = TheMovieDbOntology.getImageVertexId(key.getImagePath());
        VertexBuilder m = prepareVertex(vertexId, visibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(m, conceptType, visibility);
        VisalloProperties.SOURCE.addPropertyValue(m, MULTI_VALUE_KEY, SOURCE, visibility);
        StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(value.getBytes()), byte[].class);
        rawValue.store(true);
        rawValue.searchIndex(false);
        VisalloProperties.RAW.setProperty(m, rawValue, visibility);
        VisalloProperties.TITLE.addPropertyValue(m, MULTI_VALUE_KEY, "Image of " + title, visibility);
        Vertex profileImageVertex = m.save(authorizations);

        VertexBuilder sourceVertexMutation = prepareVertex(sourceVertexId, visibility);
        VisalloProperties.ENTITY_IMAGE_VERTEX_ID.setProperty(sourceVertexMutation, profileImageVertex.getId(), visibility);
        Vertex sourceVertex = sourceVertexMutation.save(authorizations);

        addEdge(edgeId, sourceVertex, profileImageVertex, edgeLabel, visibility, authorizations);

        context.getCounter(TheMovieDbImportCounters.IMAGES_PROCESSED).increment(1);
    }
}
