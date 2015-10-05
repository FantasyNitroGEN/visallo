package org.visallo.clavin;

import com.bericotech.clavin.extractor.LocationOccurrence;
import com.bericotech.clavin.gazetteer.FeatureClass;
import com.bericotech.clavin.gazetteer.FeatureCode;
import com.bericotech.clavin.gazetteer.GeoName;
import com.bericotech.clavin.resolver.LuceneLocationResolver;
import com.bericotech.clavin.resolver.ResolvedLocation;
import com.google.inject.Inject;
import org.apache.lucene.queryparser.classic.ParseException;
import org.vertexium.*;
import org.vertexium.type.GeoPoint;
import org.visallo.core.config.Configuration;
import org.visallo.core.ingest.graphProperty.TermMentionFilter;
import org.visallo.core.ingest.graphProperty.TermMentionFilterPrepareData;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionBuilder;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.PropertyType;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.vertexium.util.IterableUtils.count;

/**
 * This TermResolutionWorker uses the CLAVIN processor to refine
 * identification of location entities.
 */
@Name("CLAVIN")
@Description("Integrates the CLAVIN open source tool to resolve geo-locations")
public class ClavinTermMentionFilter extends TermMentionFilter {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ClavinTermMentionFilter.class);

    public static final String MULTI_VALUE_PROPERTY_KEY = ClavinTermMentionFilter.class.getName();

    /**
     * The CLAVIN index directory configuration key.
     */
    public static final String CLAVIN_INDEX_DIRECTORY = "clavin.indexDirectory";

    /**
     * The CLAVIN max hit depth configuration key.
     */
    public static final String CLAVIN_MAX_HIT_DEPTH = "clavin.maxHitDepth";

    /**
     * The CLAVIN max context window configuration key.
     */
    public static final String CLAVIN_MAX_CONTEXT_WINDOW = "clavin.maxContextWindow";

    /**
     * The CLAVIN use fuzzy matching configuration key.
     */
    public static final String CLAVIN_USE_FUZZY_MATCHING = "clavin.useFuzzyMatching";

    /**
     * The default max hit depth.
     */
    public static final int DEFAULT_MAX_HIT_DEPTH = 5;

    /**
     * The default max context window.
     */
    public static final int DEFAULT_MAX_CONTENT_WINDOW = 5;

    /**
     * The default fuzzy matching.
     */
    public static final boolean DEFAULT_FUZZY_MATCHING = false;

    private static final String CONFIG_EXCLUDED_IRI_PREFIX = "clavin.excludeIri";

    private LuceneLocationResolver resolver;
    private boolean fuzzy;
    private Set<String> targetConcepts;
    private OntologyRepository ontologyRepository;
    private String stateIri;
    private String countryIri;
    private String cityIri;
    private String geoLocationIri;
    private User user;
    private String artifactHasEntityIri;
    private WorkspaceRepository workspaceRepository;

    @Override
    public void prepare(TermMentionFilterPrepareData termMentionFilterPrepareData) throws Exception {
        super.prepare(termMentionFilterPrepareData);

        LOGGER.info("Configuring CLAVIN Location Resolution.");
        prepareIris();
        prepareClavinLuceneIndex(getConfiguration());
        prepareFuzzy(getConfiguration());
        prepareTargetConcepts(getConfiguration());
        user = termMentionFilterPrepareData.getUser();
    }

    public void prepareTargetConcepts(Configuration config) {
        Set<String> excludedIris = getExcludedIris(config);

        Set<String> conceptsWithGeoLocationProperty = new HashSet<>();
        for (Concept concept : ontologyRepository.getConceptsWithProperties()) {
            for (OntologyProperty property : concept.getProperties()) {
                String iri = concept.getIRI();
                if (property.getDataType() == PropertyType.GEO_LOCATION && !excludedIris.contains(iri)) {
                    conceptsWithGeoLocationProperty.add(iri);
                    break;
                }
            }
        }
        targetConcepts = Collections.unmodifiableSet(conceptsWithGeoLocationProperty);
    }

    private Set<String> getExcludedIris(Configuration config) {
        Set<String> excludedIris = new HashSet<>();
        excludedIris.addAll(config.getSubset(CONFIG_EXCLUDED_IRI_PREFIX).values());
        return excludedIris;
    }

    public void prepareFuzzy(Configuration config) {
        String fuzzyStr = config.get(CLAVIN_USE_FUZZY_MATCHING, null);
        if (fuzzyStr != null) {
            fuzzyStr = fuzzyStr.trim();
        }
        if (fuzzyStr != null && Boolean.TRUE.toString().equalsIgnoreCase(fuzzyStr) ||
                Boolean.FALSE.toString().equalsIgnoreCase(fuzzyStr)) {
            fuzzy = Boolean.parseBoolean(fuzzyStr);
            LOGGER.debug("Found %s: %s. fuzzy=%s", CLAVIN_USE_FUZZY_MATCHING, fuzzyStr, fuzzy);
        } else {
            LOGGER.debug("%s not configured. Using default: %s", CLAVIN_USE_FUZZY_MATCHING, DEFAULT_FUZZY_MATCHING);
            fuzzy = DEFAULT_FUZZY_MATCHING;
        }
    }

    public void prepareClavinLuceneIndex(Configuration config) throws IOException, ParseException {
        String idxDirPath = config.get(CLAVIN_INDEX_DIRECTORY, null);
        if (idxDirPath == null || idxDirPath.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format("%s must be configured.", CLAVIN_INDEX_DIRECTORY));
        }
        LOGGER.debug("Configuring CLAVIN index [%s]: %s", CLAVIN_INDEX_DIRECTORY, idxDirPath);
        File indexDirectory = new File(idxDirPath);
        if (!indexDirectory.exists() || !indexDirectory.isDirectory()) {
            throw new IllegalArgumentException(String.format("CLAVIN index cannot be found at configured (%s) location: %s",
                    CLAVIN_INDEX_DIRECTORY, idxDirPath));
        }

        int maxHitDepth = config.getInt(CLAVIN_MAX_HIT_DEPTH);
        if (maxHitDepth < 1) {
            LOGGER.debug("Found %s of %d. Using default: %d", CLAVIN_MAX_HIT_DEPTH, maxHitDepth, DEFAULT_MAX_HIT_DEPTH);
            maxHitDepth = DEFAULT_MAX_HIT_DEPTH;
        }
        int maxContextWindow = config.getInt(CLAVIN_MAX_CONTEXT_WINDOW);
        if (maxContextWindow < 1) {
            LOGGER.debug("Found %s of %d. Using default: %d", CLAVIN_MAX_CONTEXT_WINDOW, maxContextWindow, DEFAULT_MAX_CONTENT_WINDOW);
            maxContextWindow = DEFAULT_MAX_CONTENT_WINDOW;
        }

        resolver = new LuceneLocationResolver(indexDirectory, maxHitDepth, maxContextWindow);
    }

    private void prepareIris() {
        artifactHasEntityIri = ontologyRepository.getRequiredRelationshipIRIByIntent("artifactHasEntity");
        stateIri = ontologyRepository.getRequiredConceptIRIByIntent("state");
        countryIri = ontologyRepository.getRequiredConceptIRIByIntent("country");
        cityIri = ontologyRepository.getRequiredConceptIRIByIntent("city");
        geoLocationIri = ontologyRepository.getRequiredPropertyIRIByIntent("geoLocation");
    }

    @Override
    public void apply(Vertex outVertex, Iterable<Vertex> termMentions, Authorizations authorizations) throws IOException, ParseException {
        List<LocationOccurrence> locationOccurrences = getLocationOccurrencesFromTermMentions(termMentions);
        LOGGER.info("Found %d Locations in %d terms.", locationOccurrences.size(), count(termMentions));
        List<ResolvedLocation> resolvedLocationNames = resolver.resolveLocations(locationOccurrences, fuzzy);
        LOGGER.info("Resolved %d Locations", resolvedLocationNames.size());

        if (resolvedLocationNames.isEmpty()) {
            return;
        }

        Map<Integer, ResolvedLocation> resolvedLocationOffsetMap = new HashMap<>();
        for (ResolvedLocation resolvedLocation : resolvedLocationNames) {
            // assumes start/end positions are real, i.e., unique start positions for each extracted term
            resolvedLocationOffsetMap.put(resolvedLocation.getLocation().getPosition(), resolvedLocation);
        }

        ResolvedLocation loc;
        String processId = getClass().getName();
        for (Vertex termMention : termMentions) {
            loc = resolvedLocationOffsetMap.get((int) VisalloProperties.TERM_MENTION_START_OFFSET.getPropertyValue(termMention, 0));
            if (isLocation(termMention) && loc != null) {
                String id = String.format("CLAVIN-%d", loc.getGeoname().getGeonameID());
                GeoPoint geoPoint = new GeoPoint(loc.getGeoname().getLatitude(), loc.getGeoname().getLongitude(), VisalloProperties.TERM_MENTION_TITLE.getPropertyValue(termMention));
                String title = toSign(loc);
                String termMentionConceptType = VisalloProperties.TERM_MENTION_CONCEPT_TYPE.getPropertyValue(termMention);
                String conceptType = getOntologyClassUri(loc, termMentionConceptType);

                VisibilityJson outVertexVisibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(outVertex);
                Metadata metadata = new Metadata();
                VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, outVertexVisibilityJson, getVisibilityTranslator().getDefaultVisibility());
                ElementBuilder<Vertex> resolvedToVertexBuilder = getGraph().prepareVertex(id, outVertex.getVisibility())
                        .addPropertyValue(MULTI_VALUE_PROPERTY_KEY, geoLocationIri, geoPoint, metadata, outVertex.getVisibility());
                VisalloProperties.CONCEPT_TYPE.setProperty(resolvedToVertexBuilder, conceptType, metadata, outVertex.getVisibility());
                VisalloProperties.SOURCE.addPropertyValue(resolvedToVertexBuilder, MULTI_VALUE_PROPERTY_KEY, "CLAVIN", metadata, outVertex.getVisibility());
                VisalloProperties.TITLE.addPropertyValue(resolvedToVertexBuilder, MULTI_VALUE_PROPERTY_KEY, title, metadata, outVertex.getVisibility());
                VisalloProperties.VISIBILITY_JSON.setProperty(resolvedToVertexBuilder, outVertexVisibilityJson, metadata, outVertex.getVisibility());
                Vertex resolvedToVertex = resolvedToVertexBuilder.save(authorizations);
                getGraph().flush();

                String edgeId = outVertex.getId() + "-" + artifactHasEntityIri + "-" + resolvedToVertex.getId();
                Edge resolvedEdge = getGraph().prepareEdge(edgeId, outVertex, resolvedToVertex, artifactHasEntityIri, outVertex.getVisibility()).save(authorizations);
                VisalloProperties.VISIBILITY_JSON.setProperty(resolvedEdge, outVertexVisibilityJson, metadata, outVertex.getVisibility(), authorizations);
                VisibilityJson visibilityJson = VisalloProperties.TERM_MENTION_VISIBILITY_JSON.getPropertyValue(termMention);
                if (visibilityJson != null && visibilityJson.getWorkspaces().size() > 0) {
                    Set<String> workspaceIds = visibilityJson.getWorkspaces();
                    for (String workspaceId : workspaceIds) {
                        workspaceRepository.updateEntityOnWorkspace(workspaceRepository.findById(workspaceId, user), id, false, null, user);
                    }
                }

                Vertex resolvedMention = new TermMentionBuilder(termMention, outVertex)
                        .resolvedTo(resolvedToVertex, resolvedEdge)
                        .title(title)
                        .conceptIri(conceptType)
                        .process(processId)
                        .visibilityJson(VisalloProperties.TERM_MENTION_VISIBILITY_JSON.getPropertyValue(termMention))
                        .save(getGraph(), getVisibilityTranslator(), user, authorizations);

                LOGGER.debug("Replacing original location [%s] with resolved location [%s]", termMention.getId(), resolvedMention.getId());
            }
        }
    }

    private String toSign(final ResolvedLocation location) {
        GeoName geoname = location.getGeoname();
        return String.format("%s (%s, %s)", geoname.getName(), geoname.getPrimaryCountryCode(), geoname.getAdmin1Code());
    }

    private boolean isLocation(final Vertex mention) {
        return targetConcepts.contains(VisalloProperties.TERM_MENTION_CONCEPT_TYPE.getPropertyValue(mention));
    }

    private List<LocationOccurrence> getLocationOccurrencesFromTermMentions(final Iterable<Vertex> termMentions) {
        List<LocationOccurrence> locationOccurrences = new ArrayList<>();

        for (Vertex termMention : termMentions) {
            if (isLocation(termMention)) {
                locationOccurrences.add(new LocationOccurrence(VisalloProperties.TERM_MENTION_TITLE.getPropertyValue(termMention), (int) VisalloProperties.TERM_MENTION_START_OFFSET.getPropertyValue(termMention, 0)));
            }
        }
        return locationOccurrences;
    }

    public String getOntologyClassUri(final ResolvedLocation location, final String defaultValue) {
        String uri = defaultValue;
        FeatureClass featureClass = location.getGeoname().getFeatureClass();
        FeatureCode featureCode = location.getGeoname().getFeatureCode();
        if (featureClass == null) {
            featureClass = FeatureClass.NULL;
        }
        if (featureCode == null) {
            featureCode = FeatureCode.NULL;
        }
        switch (featureClass) {
            case A:
                switch (featureCode) {
                    case ADM1:
                        uri = stateIri;
                        break;
                    case PCLI:
                        uri = countryIri;
                        break;
                }
                break;
            case P:
                uri = cityIri;
                break;
        }
        return uri;
    }

    @Inject
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Inject
    public void setWorkspaceRepository(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }
}
