package org.visallo.core.model.ontology;

import org.json.JSONArray;
import org.json.JSONException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.vertexium.Authorizations;
import org.vertexium.query.Query;
import org.visallo.core.model.properties.types.VisalloProperty;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.web.clientapi.model.ClientApiOntology;

import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

public interface OntologyRepository {
    String ENTITY_CONCEPT_IRI = "http://www.w3.org/2002/07/owl#Thing";
    String ROOT_CONCEPT_IRI = "http://visallo.org#root";
    String TYPE_RELATIONSHIP = "relationship";
    String TYPE_CONCEPT = "concept";
    String TYPE_PROPERTY = "property";
    String VISIBILITY_STRING = "ontology";
    String CONFIG_INTENT_CONCEPT_PREFIX = "ontology.intent.concept.";
    String CONFIG_INTENT_RELATIONSHIP_PREFIX = "ontology.intent.relationship.";
    String CONFIG_INTENT_PROPERTY_PREFIX = "ontology.intent.property.";
    VisalloVisibility VISIBILITY = new VisalloVisibility(VISIBILITY_STRING);

    void clearCache();

    Iterable<Relationship> getRelationships();

    Iterable<OntologyProperty> getProperties();

    String getDisplayNameForLabel(String relationshipIRI);

    OntologyProperty getPropertyByIRI(String propertyIRI);

    Relationship getRelationshipByIRI(String propertyIRI);

    boolean hasRelationshipByIRI(String relationshipIRI);

    Iterable<Concept> getConceptsWithProperties();

    Concept getEntityConcept();

    Concept getParentConcept(Concept concept);

    Concept getConceptByIRI(String conceptIRI);

    Set<Concept> getConceptAndAllChildrenByIri(String conceptIRI);

    Set<Concept> getConceptAndAllChildren(Concept concept);

    Set<Relationship> getRelationshipAndAllChildren(Relationship relationship);

    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir);

    Relationship getOrCreateRelationshipType(
            Relationship parent,
            Iterable<Concept> domainConcepts,
            Iterable<Concept> rangeConcepts,
            String relationshipIRI,
            String displayName
    );

    OntologyProperty getOrCreateProperty(OntologyPropertyDefinition ontologyPropertyDefinition);

    OWLOntologyManager createOwlOntologyManager(OWLOntologyLoaderConfiguration config, IRI excludeDocumentIRI) throws Exception;

    void resolvePropertyIds(JSONArray filterJson) throws JSONException;

    void importResourceOwl(Class baseClass, String fileName, String iri, Authorizations authorizations);

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

    List<OntologyProperty> getPropertiesByIntent(String intent);

    OntologyProperty getRequiredPropertyByIntent(String intent);

    String getRequiredPropertyIRIByIntent(String intent);

    boolean isOntologyDefined(String iri);

    OntologyProperty getDependentPropertyParent(String iri);

    void addConceptTypeFilterToQuery(Query query, String conceptTypeIri, boolean includeChildNodes);

    void addEdgeLabelFilterToQuery(Query query, String edgeLabel, boolean includeChildNodes);
}
