package org.visallo.core.model.ontology;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import org.json.JSONArray;
import org.json.JSONException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.vertexium.Authorizations;
import org.vertexium.query.Query;
import org.visallo.core.model.properties.types.VisalloProperty;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.clientapi.model.ClientApiOntology;

import java.io.File;
import java.util.Collection;
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

    void clearCache(String workspaceId);

    Iterable<Relationship> getRelationships();

    Iterable<Relationship> getRelationships(Iterable<String> ids, User user, String workspaceId);

    Iterable<Relationship> getRelationships(User user, String workspaceId);

    Iterable<OntologyProperty> getProperties();

    Iterable<OntologyProperty> getProperties(Iterable<String> ids, User user, String workspaceId);

    Iterable<OntologyProperty> getProperties(User user, String workspaceId);

    String getDisplayNameForLabel(String relationshipIRI);

    String getDisplayNameForLabel(String relationshipIRI, User user, String workspaceId);

    OntologyProperty getPropertyByIRI(String propertyIRI);

    OntologyProperty getPropertyByIRI(String propertyIRI, User user, String workspaceId);

    OntologyProperty getRequiredPropertyByIRI(String propertyIRI);

    OntologyProperty getRequiredPropertyByIRI(String propertyIRI, User user, String workspaceId);

    Relationship getRelationshipByIRI(String propertyIRI);

    Relationship getRelationshipByIRI(String propertyIRI, User user, String workspaceId);

    boolean hasRelationshipByIRI(String relationshipIRI);

    boolean hasRelationshipByIRI(String relationshipIRI, User user, String workspaceId);

    Iterable<Concept> getConceptsWithProperties(User user, String workspaceId);

    Iterable<Concept> getConceptsWithProperties();

    Concept getRootConcept();

    Concept getRootConcept(User user, String workspaceId);

    Concept getEntityConcept();

    Concept getEntityConcept(User user, String workspaceId);

    Concept getParentConcept(Concept concept);

    Concept getParentConcept(Concept concept, User user, String workspaceId);

    Concept getConceptByIRI(String conceptIRI);

    Concept getConceptByIRI(String conceptIRI, User user, String workspaceId);

    Set<Concept> getConceptAndAllChildrenByIri(String conceptIRI);

    Set<Concept> getConceptAndAllChildrenByIri(String conceptIRI, User user, String workspaceId);

    Set<Concept> getConceptAndAllChildren(Concept concept);

    Set<Concept> getConceptAndAllChildren(Concept concept, User user, String workspaceId);

    Set<Relationship> getRelationshipAndAllChildren(Relationship relationship);

    Set<Relationship> getRelationshipAndAllChildren(Relationship relationship, User user, String workspaceId);

    Iterable<Concept> getConcepts(Iterable<String> ids, User user, String workspaceId);

    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir);

    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir, User user, String workspaceId);

    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir, boolean deleteChangeableProperties);

    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir, boolean deleteChangeableProperties, User user, String workspaceId);

    Relationship getOrCreateRelationshipType(
            Relationship parent,
            Iterable<Concept> domainConcepts,
            Iterable<Concept> rangeConcepts,
            String relationshipIRI
    );

    Relationship getOrCreateRelationshipType(
            Relationship parent,
            Iterable<Concept> domainConcepts,
            Iterable<Concept> rangeConcepts,
            String relationshipIRI,
            boolean deleteChangeableProperties
    );

    Relationship getOrCreateRelationshipType(
            Relationship parent,
            Iterable<Concept> domainConcepts,
            Iterable<Concept> rangeConcepts,
            String relationshipIRI,
            boolean deleteChangeableProperties,
            User user,
            String workspaceId
    );

    OntologyProperty getOrCreateProperty(OntologyPropertyDefinition ontologyPropertyDefinition);

    OWLOntologyManager createOwlOntologyManager(OWLOntologyLoaderConfiguration config, IRI excludeDocumentIRI) throws Exception;

    void resolvePropertyIds(JSONArray filterJson) throws JSONException;

    void resolvePropertyIds(JSONArray filterJson, User user, String workspaceId) throws JSONException;

    void importResourceOwl(Class baseClass, String fileName, String iri, Authorizations authorizations);

    void importFile(File inFile, IRI documentIRI, Authorizations authorizations) throws Exception;

    void importFileData(byte[] inFileData, IRI documentIRI, File inDir, Authorizations authorizations) throws Exception;

    void writePackage(File file, IRI documentIRI, Authorizations authorizations) throws Exception;

    ClientApiOntology getClientApiObject();

    ClientApiOntology getClientApiObject(User user, String workspaceId);

    String guessDocumentIRIFromPackage(File inFile) throws Exception;

    Concept getConceptByIntent(String intent);

    Concept getConceptByIntent(String intent, User user, String workspaceId);

    String getConceptIRIByIntent(String intent);

    String getConceptIRIByIntent(String intent, User user, String workspaceId);

    Concept getRequiredConceptByIntent(String intent);

    Concept getRequiredConceptByIntent(String intent, User user, String workspaceId);

    Concept getRequiredConceptByIRI(String iri);

    Concept getRequiredConceptByIRI(String iri, User user, String workspaceId);

    String getRequiredConceptIRIByIntent(String intent);

    String getRequiredConceptIRIByIntent(String intent, User user, String workspaceId);

    Relationship getRelationshipByIntent(String intent);

    Relationship getRelationshipByIntent(String intent, User user, String workspaceId);

    String getRelationshipIRIByIntent(String intent);

    String getRelationshipIRIByIntent(String intent, User user, String workspaceId);

    Relationship getRequiredRelationshipByIntent(String intent);

    Relationship getRequiredRelationshipByIntent(String intent, User user, String workspaceId);

    String getRequiredRelationshipIRIByIntent(String intent);

    String getRequiredRelationshipIRIByIntent(String intent, User user, String workspaceId);

    OntologyProperty getPropertyByIntent(String intent);

    OntologyProperty getPropertyByIntent(String intent, User user, String workspaceId);

    String getPropertyIRIByIntent(String intent);

    String getPropertyIRIByIntent(String intent, User user, String workspaceId);

    <T extends VisalloProperty> T getVisalloPropertyByIntent(String intent, Class<T> visalloPropertyType);

    <T extends VisalloProperty> T getVisalloPropertyByIntent(String intent, Class<T> visalloPropertyType, User user, String workspaceId);

    <T extends VisalloProperty> T getRequiredVisalloPropertyByIntent(String intent, Class<T> visalloPropertyType);

    <T extends VisalloProperty> T getRequiredVisalloPropertyByIntent(String intent, Class<T> visalloPropertyType, User user, String workspaceId);

    List<OntologyProperty> getPropertiesByIntent(String intent);

    List<OntologyProperty> getPropertiesByIntent(String intent, User user, String workspaceId);

    OntologyProperty getRequiredPropertyByIntent(String intent);

    OntologyProperty getRequiredPropertyByIntent(String intent, User user, String workspaceId);

    String getRequiredPropertyIRIByIntent(String intent);

    String getRequiredPropertyIRIByIntent(String intent, User user, String workspaceId);

    boolean isOntologyDefined(String iri);

    OntologyProperty getDependentPropertyParent(String iri);

    OntologyProperty getDependentPropertyParent(String iri, User user, String workspaceId);

    void addConceptTypeFilterToQuery(Query query, String conceptTypeIri, boolean includeChildNodes);

    void addConceptTypeFilterToQuery(Query query, String conceptTypeIri, boolean includeChildNodes, User user, String workspaceId);

    void addConceptTypeFilterToQuery(Query query, Collection<ElementTypeFilter> filters);

    void addConceptTypeFilterToQuery(Query query, Collection<ElementTypeFilter> filters, User user, String workspaceId);

    void addEdgeLabelFilterToQuery(Query query, String edgeLabel, boolean includeChildNodes);

    void addEdgeLabelFilterToQuery(Query query, String edgeLabel, boolean includeChildNodes, User user, String workspaceId);

    void addEdgeLabelFilterToQuery(Query query, Collection<ElementTypeFilter> filters);

    void addEdgeLabelFilterToQuery(Query query, Collection<ElementTypeFilter> filters, User user, String workspaceId);

    void updatePropertyDependentIris(OntologyProperty property, Collection<String> dependentPropertyIris);

    void updatePropertyDependentIris(OntologyProperty property, Collection<String> dependentPropertyIris, User user, String workspaceId);

    void updatePropertyDomainIris(OntologyProperty property, Set<String> domainIris);

    void updatePropertyDomainIris(OntologyProperty property, Set<String> domainIris, User user, String workspaceId);

    String generateDynamicIri(String displayName, String workspaceId);

    class ElementTypeFilter implements ClientApiObject {
        public String iri;
        public boolean includeChildNodes;

        public ElementTypeFilter() {

        }

        public ElementTypeFilter(String iri, boolean includeChildNodes) {
            this.iri = iri;
            this.includeChildNodes = includeChildNodes;
        }
    }
}
