package org.visallo.core.model.ontology;

import org.visallo.core.model.properties.types.VisalloProperty;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.json.JSONArray;
import org.json.JSONException;
import org.vertexium.Authorizations;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.File;
import java.io.OutputStream;
import java.util.Set;

public interface OntologyRepository {
    public static final String ENTITY_CONCEPT_IRI = "http://www.w3.org/2002/07/owl#Thing";
    public static final String ROOT_CONCEPT_IRI = "http://visallo.org#root";
    public static final String TYPE_RELATIONSHIP = "relationship";
    public static final String TYPE_CONCEPT = "concept";
    public static final String TYPE_PROPERTY = "property";
    public static final String VISIBILITY_STRING = "ontology";
    public static final String CONFIG_INTENT_CONCEPT_PREFIX = "ontology.intent.concept.";
    public static final String CONFIG_INTENT_RELATIONSHIP_PREFIX = "ontology.intent.relationship.";
    public static final String CONFIG_INTENT_PROPERTY_PREFIX = "ontology.intent.property.";
    public static final VisalloVisibility VISIBILITY = new VisalloVisibility(VISIBILITY_STRING);

    void clearCache();

    Iterable<Relationship> getRelationships();

    Iterable<OntologyProperty> getProperties();

    String getDisplayNameForLabel(String relationshipIRI);

    OntologyProperty getPropertyByIRI(String propertyIRI);

    boolean hasRelationshipByIRI(String relationshipIRI);

    Iterable<Concept> getConceptsWithProperties();

    Concept getEntityConcept();

    Concept getParentConcept(Concept concept);

    Concept getConceptByIRI(String conceptIRI);

    Set<Concept> getConceptAndAllChildrenByIri(String conceptIRI);

    Set<Concept> getConceptAndAllChildren(Concept concept);

    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir);

    Relationship getOrCreateRelationshipType(Iterable<Concept> domainConcepts, Iterable<Concept> rangeConcepts, String relationshipIRI, String displayName, String[] intents, boolean userVisible);

    OWLOntologyManager createOwlOntologyManager(OWLOntologyLoaderConfiguration config, IRI excludeDocumentIRI) throws Exception;

    void resolvePropertyIds(JSONArray filterJson) throws JSONException;

    void importFile(File inFile, IRI documentIRI, Authorizations authorizations) throws Exception;

    void importFileData(byte[] inFileData, IRI documentIRI, File inDir, Authorizations authorizations) throws Exception;

    void exportOntology(OutputStream out, IRI documentIRI) throws Exception;

    void writePackage(File file, IRI documentIRI, Authorizations authorizations) throws Exception;

    ClientApiOntology getClientApiObject();

    String guessDocumentIRIFromPackage(File inFile) throws Exception;

    Concept getConceptByIntent(String intent);

    String getConceptIRIByIntent(String intent);

    Concept getRequiredConceptByIntent(String intent);

    Concept getRequiredConceptByIRI(String iri);

    String getRequiredConceptIRIByIntent(String intent);

    Relationship getRelationshipByIntent(String intent);

    String getRelationshipIRIByIntent(String intent);

    Relationship getRequiredRelationshipByIntent(String intent);

    String getRequiredRelationshipIRIByIntent(String intent);

    OntologyProperty getPropertyByIntent(String intent);

    String getPropertyIRIByIntent(String intent);

    <T extends VisalloProperty> T getVisalloPropertyByIntent(String intent, Class<T> visalloPropertyType);

    <T extends VisalloProperty> T getRequiredVisalloPropertyByIntent(String intent, Class<T> visalloPropertyType);

    OntologyProperty getRequiredPropertyByIntent(String intent);

    String getRequiredPropertyIRIByIntent(String intent);

    boolean isOntologyDefined(String iri);
}
