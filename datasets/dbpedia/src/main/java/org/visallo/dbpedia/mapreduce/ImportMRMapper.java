package org.visallo.dbpedia.mapreduce;

import org.visallo.core.config.Configuration;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.lock.NonLockingLockRepository;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.security.DirectVisibilityTranslator;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.dbpedia.mapreduce.model.LineData;
import org.visallo.dbpedia.mapreduce.model.LinkValue;
import org.visallo.vertexium.mapreduce.VisalloElementMapperBase;
import org.visallo.vertexium.model.ontology.VertexiumOntologyRepository;
import org.visallo.vertexium.model.user.AccumuloAuthorizationRepository;
import org.visallo.wikipedia.WikipediaConstants;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.vertexium.Metadata;
import org.vertexium.Vertex;
import org.vertexium.VertexBuilder;
import org.vertexium.Visibility;
import org.vertexium.accumulo.AccumuloAuthorizations;
import org.vertexium.accumulo.mapreduce.VertexiumMRUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ImportMRMapper extends VisalloElementMapperBase<LongWritable, Text> {
    private static final String DBPEDIA_ID_PREFIX = "DBPEDIA_";
    private Counter linesProcessedCounter;
    private VisibilityTranslator visibilityTranslator;
    private Visibility visibility;
    private Visibility defaultVisibility;
    private AccumuloAuthorizations authorizations;
    private VertexiumOntologyRepository ontologyRepository;
    private static final Map<String, Integer> conceptTypeDepthCache = new HashMap<>();

    public static String getDbpediaEntityVertexId(String pageTitle) {
        return DBPEDIA_ID_PREFIX + pageTitle.trim().toLowerCase();
    }

    private String getEntityHasWikipediaPageEdgeId(Vertex entityVertex, Vertex pageVertex) {
        return DBPEDIA_ID_PREFIX + entityVertex.getId() + "_HAS_PAGE_" + pageVertex.getId();
    }

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);

        this.visibilityTranslator = new DirectVisibilityTranslator();
        this.visibility = this.visibilityTranslator.getDefaultVisibility();
        this.defaultVisibility = this.visibilityTranslator.getDefaultVisibility();
        this.authorizations = new AccumuloAuthorizations();
        AccumuloAuthorizationRepository authorizationRepository = new AccumuloAuthorizationRepository();
        authorizationRepository.setGraph(getGraph());
        authorizationRepository.setLockRepository(new NonLockingLockRepository());
        try {
            Map configurationMap = VertexiumMRUtils.toMap(context.getConfiguration());
            Configuration config = HashMapConfigurationLoader.load(configurationMap);
            this.ontologyRepository = new VertexiumOntologyRepository(getGraph(), config, authorizationRepository);
        } catch (Exception e) {
            throw new IOException("Could not configure vertexium ontology repository", e);
        }
        linesProcessedCounter = context.getCounter(DbpediaImportCounters.LINES_PROCESSED);
    }

    @Override
    protected void safeMap(LongWritable key, Text line, Context context) throws Exception {
        String lineString = line.toString().trim();
        try {
            if (lineString.length() == 0) {
                return;
            }
            if (lineString.startsWith("#")) {
                return;
            }

            LineData lineData = LineData.parse(lineString);

            Vertex dbpediaEntityVertex = createDbpediaEntityVertex(lineData);

            if (lineData.getValue() instanceof LinkValue) {
                LinkValue linkValue = (LinkValue) lineData.getValue();
                if (!lineData.getPropertyIri().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                    createLinkToDbpediaEntity(lineData, dbpediaEntityVertex, linkValue);
                }
            }

            linesProcessedCounter.increment(1);
        } catch (Throwable ex) {
            throw new VisalloException("Could not process line: " + lineString, ex);
        }
    }

    private void createLinkToDbpediaEntity(LineData lineData, Vertex pageVertex, LinkValue linkValue) {
        String linkedPageVertexId = WikipediaConstants.getWikipediaPageVertexId(linkValue.getPageTitle());
        VertexBuilder linkedPageVertexBuilder = prepareVertex(linkedPageVertexId, visibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(linkedPageVertexBuilder, WikipediaConstants.WIKIPEDIA_PAGE_CONCEPT_URI, visibility);

        Metadata linkedTitleMetadata = new Metadata();
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(linkedTitleMetadata, 0.1, defaultVisibility);
        VisalloProperties.TITLE.addPropertyValue(linkedPageVertexBuilder, ImportMR.MULTI_VALUE_KEY, linkValue.getPageTitle(), linkedTitleMetadata, visibility);

        Vertex linkedPageVertex = linkedPageVertexBuilder.save(authorizations);

        String label = lineData.getPropertyIri();
        String edgeId = pageVertex.getId() + "_" + label + "_" + linkedPageVertex.getId();
        addEdge(edgeId, pageVertex, linkedPageVertex, label, visibility, authorizations);
    }

    private Vertex createDbpediaEntityVertex(LineData lineData) {
        Vertex pageVertex = createPageVertex(lineData);

        String dbpediaEntityVertexId = getDbpediaEntityVertexId(lineData.getPageTitle());
        VertexBuilder entityVertexBuilder = prepareVertex(dbpediaEntityVertexId, visibility);

        Metadata conceptTypeMetadata = new Metadata();
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(conceptTypeMetadata, 0.1, defaultVisibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(entityVertexBuilder, "http://www.w3.org/2002/07/owl#Thing", conceptTypeMetadata, visibility);

        Metadata titleMetadata = new Metadata();
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(titleMetadata, 0.1, defaultVisibility);
        VisalloProperties.TITLE.addPropertyValue(entityVertexBuilder, ImportMR.MULTI_VALUE_KEY, lineData.getPageTitle(), titleMetadata, visibility);

        if (lineData.getPropertyIri().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") && lineData.getValue() instanceof LinkValue) {
            LinkValue linkValue = (LinkValue) lineData.getValue();

            Integer ontologyDepth = getConceptDepth(linkValue.getValueString());
            if (ontologyDepth != null) {
                conceptTypeMetadata = new Metadata();
                VisalloProperties.CONFIDENCE_METADATA.setMetadata(conceptTypeMetadata, 0.2 + ((double) ontologyDepth / 1000.0), defaultVisibility);
                VisalloProperties.CONCEPT_TYPE.setProperty(entityVertexBuilder, linkValue.getValueString(), conceptTypeMetadata, visibility);
            }
        }

        if (!(lineData.getValue() instanceof LinkValue)) {
            String multiValueKey = lineData.getValue().getValueString();
            entityVertexBuilder.addPropertyValue(multiValueKey, lineData.getPropertyIri(), lineData.getValue().getValue(), visibility);
        }

        Vertex entityVertex = entityVertexBuilder.save(authorizations);

        String edgeId = getEntityHasWikipediaPageEdgeId(entityVertex, pageVertex);
        addEdge(edgeId, entityVertex, pageVertex, DbpediaOntology.EDGE_LABEL_ENTITY_HAS_WIKIPEDIA_PAGE, visibility, authorizations);

        return entityVertex;
    }

    private Integer getConceptDepth(String conceptIri) {
        if (conceptTypeDepthCache.containsKey(conceptIri)) {
            return conceptTypeDepthCache.get(conceptIri);
        }

        Concept concept = this.ontologyRepository.getConceptByIRI(conceptIri);
        if (concept == null) {
            conceptTypeDepthCache.put(conceptIri, null);
            return null;
        }
        int depth = 0;
        while (true) {
            Concept parentConcept = this.ontologyRepository.getParentConcept(concept);
            if (parentConcept == null) {
                break;
            }
            depth++;
            concept = parentConcept;
        }
        conceptTypeDepthCache.put(conceptIri, depth);
        return depth;
    }

    private Vertex createPageVertex(LineData lineData) {
        String wikipediaPageVertexId = WikipediaConstants.getWikipediaPageVertexId(lineData.getPageTitle());
        VertexBuilder pageVertexBuilder = prepareVertex(wikipediaPageVertexId, visibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(pageVertexBuilder, WikipediaConstants.WIKIPEDIA_PAGE_CONCEPT_URI, visibility);

        Metadata titleMetadata = new Metadata();
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(titleMetadata, 0.1, defaultVisibility);
        VisalloProperties.TITLE.addPropertyValue(pageVertexBuilder, ImportMR.MULTI_VALUE_KEY, lineData.getPageTitle(), titleMetadata, visibility);

        return pageVertexBuilder.save(authorizations);
    }
}
