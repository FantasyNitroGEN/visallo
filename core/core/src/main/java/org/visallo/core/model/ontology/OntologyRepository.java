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

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getRelationships(User, String)} instead.
     */
    @Deprecated
    Iterable<Relationship> getRelationships();

    Iterable<Relationship> getRelationships(User user, String workspaceId);

    Iterable<Relationship> getRelationships(Iterable<String> ids, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getProperties(User, String)} instead.
     */
    Iterable<OntologyProperty> getProperties();

    Iterable<OntologyProperty> getProperties(User user, String workspaceId);

    Iterable<OntologyProperty> getProperties(Iterable<String> ids, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getDisplayNameForLabel(String, User, String)} instead.
     */
    String getDisplayNameForLabel(String relationshipIRI);

    String getDisplayNameForLabel(String relationshipIRI, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getPropertyByIRI(String, User, String)} instead.
     */
    OntologyProperty getPropertyByIRI(String propertyIRI);

    OntologyProperty getPropertyByIRI(String propertyIRI, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getRequiredPropertyByIRI(String, User, String)} instead.
     */
    OntologyProperty getRequiredPropertyByIRI(String propertyIRI);

    OntologyProperty getRequiredPropertyByIRI(String propertyIRI, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getRelationshipByIRI(String, User, String)} instead.
     */
    Relationship getRelationshipByIRI(String propertyIRI);

    Relationship getRelationshipByIRI(String propertyIRI, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #hasRelationshipByIRI(String, User, String)} instead.
     */
    boolean hasRelationshipByIRI(String relationshipIRI);

    boolean hasRelationshipByIRI(String relationshipIRI, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getConceptsWithProperties(User, String)} instead.
     */
    Iterable<Concept> getConceptsWithProperties();

    Iterable<Concept> getConceptsWithProperties(User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getRootConcept(User, String)} instead.
     */
    Concept getRootConcept();

    Concept getRootConcept(User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getEntityConcept(User, String)} instead.
     */
    Concept getEntityConcept();

    Concept getEntityConcept(User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getParentConcept(Concept, User, String)} instead.
     */
    Concept getParentConcept(Concept concept);

    Concept getParentConcept(Concept concept, User user, String workspaceId);

    Set<Concept> getAncestorConcepts(Concept concept, User user, String workspaceId);

    Set<Concept> getConceptAndAncestors(Concept concept, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getConceptByIRI(String, User, String)} instead.
     */
    Concept getConceptByIRI(String conceptIRI);

    Concept getConceptByIRI(String conceptIRI, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getConceptAndAllChildrenByIri(String, User, String)} instead.
     */
    Set<Concept> getConceptAndAllChildrenByIri(String conceptIRI);

    Set<Concept> getConceptAndAllChildrenByIri(String conceptIRI, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getConceptAndAllChildren(Concept, User, String)} instead.
     */
    Set<Concept> getConceptAndAllChildren(Concept concept);

    Set<Concept> getConceptAndAllChildren(Concept concept, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getRelationshipAndAllChildren(Relationship, User, String)} instead.
     */
    Set<Relationship> getRelationshipAndAllChildren(Relationship relationship);

    Set<Relationship> getRelationshipAndAllChildren(Relationship relationship, User user, String workspaceId);

    Relationship getParentRelationship(Relationship relationship, User user, String workspaceId);

    Set<Relationship> getAncestorRelationships(Relationship relationship, User user, String workspaceId);

    Set<Relationship> getRelationshipAndAncestors(Relationship relationship, User user, String workspaceId);

    Iterable<Concept> getConcepts(Iterable<String> ids, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getOrCreateConcept(Concept, String, String, File, User, String)} instead.
     */
    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir);

    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir, User user, String workspaceId);

    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, String glyphIconHref, File inDir, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getOrCreateConcept(Concept, String, String, File, boolean, User, String)} instead.
     */
    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir, boolean deleteChangeableProperties);

    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir, boolean deleteChangeableProperties, User user, String workspaceId);

    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, String glyphIconHref, File inDir, boolean deleteChangeableProperties, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getOrCreateRelationshipType(Relationship, Iterable, Iterable, String, boolean, User, String)} instead.
     */
    Relationship getOrCreateRelationshipType(
            Relationship parent,
            Iterable<Concept> domainConcepts,
            Iterable<Concept> rangeConcepts,
            String relationshipIRI
    );

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getOrCreateRelationshipType(Relationship, Iterable, Iterable, String, String, boolean, User, String)} instead.
     */
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

    Relationship getOrCreateRelationshipType(
            Relationship parent,
            Iterable<Concept> domainConcepts,
            Iterable<Concept> rangeConcepts,
            String relationshipIRI,
            String displayName,
            boolean deleteChangeableProperties,
            User user,
            String workspaceId
    );

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getOrCreateProperty(OntologyPropertyDefinition, User, String)} instead.
     */
    OntologyProperty getOrCreateProperty(OntologyPropertyDefinition ontologyPropertyDefinition);

    OntologyProperty getOrCreateProperty(OntologyPropertyDefinition ontologyPropertyDefinition, User user, String workspaceId);

    OWLOntologyManager createOwlOntologyManager(OWLOntologyLoaderConfiguration config, IRI excludeDocumentIRI) throws Exception;

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #resolvePropertyIds(JSONArray, User, String)} instead.
     */
    void resolvePropertyIds(JSONArray filterJson) throws JSONException;

    void resolvePropertyIds(JSONArray filterJson, User user, String workspaceId) throws JSONException;

    void importResourceOwl(Class baseClass, String fileName, String iri, Authorizations authorizations);

    void importFile(File inFile, IRI documentIRI, Authorizations authorizations) throws Exception;

    void importFileData(byte[] inFileData, IRI documentIRI, File inDir, Authorizations authorizations) throws Exception;

    void writePackage(File file, IRI documentIRI, Authorizations authorizations) throws Exception;

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getClientApiObject(User, String)} instead.
     */
    ClientApiOntology getClientApiObject();

    ClientApiOntology getClientApiObject(User user, String workspaceId);

    String guessDocumentIRIFromPackage(File inFile) throws Exception;

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getConceptByIntent(String, User, String)} instead.
     */
    Concept getConceptByIntent(String intent);

    Concept getConceptByIntent(String intent, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getConceptIRIByIntent(String, User, String)} instead.
     */
    String getConceptIRIByIntent(String intent);

    String getConceptIRIByIntent(String intent, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getRequiredConceptByIntent(String, User, String)} instead.
     */
    Concept getRequiredConceptByIntent(String intent);

    Concept getRequiredConceptByIntent(String intent, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getRequiredConceptByIRI(String, User, String)} instead.
     */
    Concept getRequiredConceptByIRI(String iri);

    Concept getRequiredConceptByIRI(String iri, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getRequiredConceptIRIByIntent(String, User, String)} instead.
     */
    String getRequiredConceptIRIByIntent(String intent);

    String getRequiredConceptIRIByIntent(String intent, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getRelationshipByIntent(String, User, String)} instead.
     */
    Relationship getRelationshipByIntent(String intent);

    Relationship getRelationshipByIntent(String intent, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getRelationshipIRIByIntent(String, User, String)} instead.
     */
    String getRelationshipIRIByIntent(String intent);

    String getRelationshipIRIByIntent(String intent, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getRequiredRelationshipByIntent(String, User, String)} instead.
     */
    Relationship getRequiredRelationshipByIntent(String intent);

    Relationship getRequiredRelationshipByIntent(String intent, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getRequiredRelationshipIRIByIntent(String, User, String)} instead.
     */
    String getRequiredRelationshipIRIByIntent(String intent);

    String getRequiredRelationshipIRIByIntent(String intent, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getPropertyByIntent(String, User, String)} instead.
     */
    OntologyProperty getPropertyByIntent(String intent);

    OntologyProperty getPropertyByIntent(String intent, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getPropertyIRIByIntent(String, User, String)} instead.
     */
    String getPropertyIRIByIntent(String intent);

    String getPropertyIRIByIntent(String intent, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getVisalloPropertyByIntent(String, Class, User, String)} instead.
     */
    <T extends VisalloProperty> T getVisalloPropertyByIntent(String intent, Class<T> visalloPropertyType);

    <T extends VisalloProperty> T getVisalloPropertyByIntent(String intent, Class<T> visalloPropertyType, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getRequiredVisalloPropertyByIntent(String, Class, User, String)} instead.
     */
    <T extends VisalloProperty> T getRequiredVisalloPropertyByIntent(String intent, Class<T> visalloPropertyType);

    <T extends VisalloProperty> T getRequiredVisalloPropertyByIntent(String intent, Class<T> visalloPropertyType, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getPropertiesByIntent(String, User, String)} instead.
     */
    List<OntologyProperty> getPropertiesByIntent(String intent);

    List<OntologyProperty> getPropertiesByIntent(String intent, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getRequiredPropertyByIntent(String, User, String)} instead.
     */
    OntologyProperty getRequiredPropertyByIntent(String intent);

    OntologyProperty getRequiredPropertyByIntent(String intent, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getRequiredPropertyIRIByIntent(String, User, String)} instead.
     */
    String getRequiredPropertyIRIByIntent(String intent);

    String getRequiredPropertyIRIByIntent(String intent, User user, String workspaceId);

    /**
     * @deprecated This method was used to avoid reimporting ontologies. It is no longer needed since MD5s of imported ontologies
     *             will be kept and if an ontology has not changed it will not be imported again.
     */
    @Deprecated
    boolean isOntologyDefined(String iri);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #getDependentPropertyParent(String, User, String)} instead.
     */
    OntologyProperty getDependentPropertyParent(String iri);

    OntologyProperty getDependentPropertyParent(String iri, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #addConceptTypeFilterToQuery(Query, String, boolean, User, String)} instead.
     */
    void addConceptTypeFilterToQuery(Query query, String conceptTypeIri, boolean includeChildNodes);

    void addConceptTypeFilterToQuery(Query query, String conceptTypeIri, boolean includeChildNodes, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #addConceptTypeFilterToQuery(Query, Collection, User, String)} instead.
     */
    void addConceptTypeFilterToQuery(Query query, Collection<ElementTypeFilter> filters);

    void addConceptTypeFilterToQuery(Query query, Collection<ElementTypeFilter> filters, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #addEdgeLabelFilterToQuery(Query, String, boolean, User, String)} instead.
     */
    void addEdgeLabelFilterToQuery(Query query, String edgeLabel, boolean includeChildNodes);

    void addEdgeLabelFilterToQuery(Query query, String edgeLabel, boolean includeChildNodes, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #addEdgeLabelFilterToQuery(Query, Collection, User, String)} instead.
     */
    void addEdgeLabelFilterToQuery(Query query, Collection<ElementTypeFilter> filters);

    void addEdgeLabelFilterToQuery(Query query, Collection<ElementTypeFilter> filters, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #updatePropertyDependentIris(OntologyProperty, Collection, User, String)} instead.
     */
    void updatePropertyDependentIris(OntologyProperty property, Collection<String> dependentPropertyIris);

    void updatePropertyDependentIris(OntologyProperty property, Collection<String> dependentPropertyIris, User user, String workspaceId);

    /**
     * @deprecated  With the addition of ontology sandboxing, ontology elements must now be retrieved with
     *              the context of a user and a workspace</br>
     *              {will be removed in next version} </br>
     *              use {@link #updatePropertyDomainIris(OntologyProperty, Set, User, String)} instead.
     */
    void updatePropertyDomainIris(OntologyProperty property, Set<String> domainIris);

    void updatePropertyDomainIris(OntologyProperty property, Set<String> domainIris, User user, String workspaceId);

    String generateDynamicIri(Class type, String displayName, String workspaceId);

    void publishConcept(Concept concept, User user, String workspaceId);

    void publishRelationship(Relationship relationship, User user, String workspaceId);

    void publishProperty(OntologyProperty property, User user, String workspaceId);

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
