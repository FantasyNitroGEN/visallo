package org.visallo.core.model.audit;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.PropertyJustificationMetadata;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.PropertyType;
import org.json.JSONObject;
import com.v5analytics.simpleorm.SimpleOrmContext;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.type.GeoPoint;
import org.vertexium.util.IterableUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.visallo.core.model.properties.VisalloProperties.CONCEPT_TYPE;
import static org.visallo.core.model.properties.VisalloProperties.TITLE;

public class SimpleOrmAuditRepository extends AuditRepository {
    private final SimpleOrmSession simpleOrmSession;
    private final Configuration configuration;
    private final OntologyRepository ontologyRepository;
    private final UserRepository userRepository;

    @Inject
    public SimpleOrmAuditRepository(
            final SimpleOrmSession simpleOrmSession,
            final Configuration configuration,
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository
    ) {
        this.simpleOrmSession = simpleOrmSession;
        this.configuration = configuration;
        this.ontologyRepository = ontologyRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Iterable<Audit> getAudits(String vertexId, String workspaceId, Authorizations authorizations) {
        SimpleOrmContext simpleOrmContext = userRepository.getSimpleOrmContext(authorizations, workspaceId);
        return this.simpleOrmSession.findByIdStartsWith(Audit.class, vertexId, simpleOrmContext);
    }

    public Audit createAudit(AuditAction auditAction, String vertexId, String process, String comment, User user) {
        checkNotNull(vertexId, "vertexId cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");
        checkNotNull(process, "process cannot be null");

        Audit audit = new AuditEntity(vertexId);
        audit
                .setUser(user)
                .setAction(auditAction)
                .setComment(comment);

        if (process.length() > 0) {
            audit.setProcess(process);
        }

        return audit;
    }

    @Override
    public Audit auditVertex(AuditAction auditAction, String vertexId, String process, String comment, User user, Visibility visibility) {
        Audit audit = createAudit(auditAction, vertexId, process, comment, user);
        simpleOrmSession.save(audit, visibility.getVisibilityString(), user.getSimpleOrmContext());
        return audit;
    }

    @Override
    public Audit auditEntityProperty(
            AuditAction action,
            String id,
            String propertyKey,
            String propertyName,
            Object oldValue,
            Object newValue,
            String process,
            String comment,
            Metadata metadata,
            User user,
            Visibility visibility) {
        checkNotNull(action, "action cannot be null");
        checkNotNull(id, "id cannot be null");
        checkNotNull(propertyName, "propertyName cannot be null");
        checkArgument(propertyName.length() > 0, "property name cannot be empty");
        checkNotNull(process, "process cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");

        AuditProperty audit = new AuditProperty(id, propertyKey, propertyName);
        visibility = orVisibility(visibility);
        audit
                .setUser(user)
                .setAction(action)
                .setComment(comment)
                .setProcess(process);

        if (oldValue != null) {
            if (oldValue instanceof GeoPoint) {
                String val = String.format("POINT(%f,%f)", ((GeoPoint) oldValue).getLatitude(), ((GeoPoint) oldValue).getLongitude());
                audit.setPreviousValue(val);
            } else {
                String convertedValue = checkAndConvertForDateType(propertyName, oldValue);
                audit.setPreviousValue(convertedValue != null ? convertedValue : oldValue.toString());
            }
        }
        if (action == AuditAction.DELETE) {
            audit.setNewValue("");
        } else {
            if (newValue instanceof GeoPoint) {
                String val = String.format("POINT(%f,%f)", ((GeoPoint) newValue).getLatitude(), ((GeoPoint) newValue).getLongitude());
                audit.setNewValue(val);
            } else {
                String convertedValue = checkAndConvertForDateType(propertyName, newValue);
                audit.setNewValue(convertedValue != null ? convertedValue : newValue.toString());
            }
        }

        if (metadata != null && !metadata.entrySet().isEmpty()) {
            audit.setPropertyMetadata(jsonMetadata(metadata));
        }

        simpleOrmSession.save(audit, visibility.getVisibilityString(), user.getSimpleOrmContext());
        return audit;
    }

    @Override
    public Audit auditRelationship(
            AuditAction action,
            Vertex sourceVertex,
            Vertex destVertex,
            Edge edge,
            String process,
            String comment,
            User user,
            Visibility visibility) {
        checkNotNull(action, "action cannot be null");
        checkNotNull(sourceVertex, "sourceVertex cannot be null");
        checkNotNull(destVertex, "destVertex cannot be null");
        checkNotNull(edge, "edge cannot be null");
        checkNotNull(process, "process cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");

        AuditRelationship audit = new AuditRelationship(edge.getId());
        visibility = orVisibility(visibility);

        String displayLabel = ontologyRepository.getDisplayNameForLabel(edge.getLabel());
        checkNotNull(displayLabel, "Could not find display name for label '" + edge.getLabel() + "' on edge " + edge.getId());

        audit
                .setUser(user)
                .setAction(action)
                .setComment(comment)
                .setProcess(process);

        Iterable<String> sourceTitleIterable = TITLE.getPropertyValues(sourceVertex);
        String sourceTitle = "";
        if (IterableUtils.count(sourceTitleIterable) != 0) {
            sourceTitle = IterableUtils.toList(sourceTitleIterable).get(IterableUtils.count(sourceTitleIterable) - 1);
        }

        Iterable<String> destTitleIterable = TITLE.getPropertyValues(destVertex);
        String destTitle = "";
        if (IterableUtils.count(destTitleIterable) != 0) {
            destTitle = IterableUtils.toList(destTitleIterable).get(IterableUtils.count(destTitleIterable) - 1);
        }

        String sourceVertexConceptType = CONCEPT_TYPE.getPropertyValue(sourceVertex);
        checkNotNull(sourceVertexConceptType, "vertex " + sourceVertex.getId() + " has a null " + CONCEPT_TYPE.getPropertyName());
        String destVertexConceptType = CONCEPT_TYPE.getPropertyValue(destVertex);
        checkNotNull(destVertexConceptType, "vertex " + destVertex.getId() + " has a null " + CONCEPT_TYPE.getPropertyName());

        audit
                .setSourceId(sourceVertex.getId())
                .setSourceType(sourceVertexConceptType)
                .setSourceTitle(sourceTitle)
                .setDestId(destVertex.getId())
                .setDestTitle(destTitle)
                .setDestType(destVertexConceptType)
                .setLabel(displayLabel);

        simpleOrmSession.save(audit, visibility.getVisibilityString(), user.getSimpleOrmContext());
        return audit;
    }

    @Override
    public Audit auditRelationshipProperty(
            AuditAction action,
            String sourceId,
            String destId,
            String propertyKey,
            String propertyName,
            Object oldValue,
            Object newValue,
            Edge edge,
            String process,
            String comment,
            User user,
            Visibility visibility
    ) {
        checkNotNull(action, "action cannot be null");
        checkNotNull(sourceId, "sourceId cannot be null");
        checkNotNull(sourceId.length() > 0, "sourceId cannot be empty");
        checkNotNull(destId, "destId cannot be null");
        checkNotNull(destId.length() > 0, "destId cannot be empty");
        checkNotNull(propertyName, "propertyName cannot be null");
        checkNotNull(propertyName.length() > 0, "propertyName cannot be empty");
        checkNotNull(edge, "edge cannot be null");
        checkNotNull(process, "process cannot be null");
        checkNotNull(comment, "comment cannot be null");
        checkNotNull(user, "user cannot be null");

        AuditProperty audit = new AuditProperty(edge.getId(), propertyKey, propertyName);
        visibility = orVisibility(visibility);

        audit
                .setUser(user)
                .setAction(action)
                .setComment(comment)
                .setProcess(process);

        propertyKey = propertyKey != null ? propertyKey : "";

        if (oldValue != null && !oldValue.equals("")) {
            String convertedValue = checkAndConvertForDateType(propertyName, oldValue);
            if (convertedValue != null) {
                oldValue = convertedValue;
            }
            audit.setPreviousValue(oldValue.toString());
        }

        if (action == AuditAction.DELETE) {
            audit.setNewValue("");
        } else {
            String convertedValue = checkAndConvertForDateType(propertyName, newValue);
            if (convertedValue != null) {
                newValue = convertedValue;
            }
            audit.setNewValue(newValue.toString());
        }

        Metadata metadata = edge.getProperty(propertyKey, propertyName).getMetadata();
        if (metadata != null && !metadata.entrySet().isEmpty()) {
            audit.setPropertyMetadata(jsonMetadata(metadata));
        }

        simpleOrmSession.save(audit, visibility.getVisibilityString(), user.getSimpleOrmContext());
        return audit;
    }

    @Override
    public Audit auditAnalyzedBy(AuditAction action, Vertex vertex, String process, User user, Visibility visibility) {
        AuditEntity audit = new AuditEntity(vertex.getId());
        audit
                .setUser(user)
                .setAction(action)
                .setProcess(process);

        audit.setAnalyzedBy(process);
        simpleOrmSession.save(audit, visibility.getVisibilityString(), user.getSimpleOrmContext());
        return audit;
    }

    @Override
    public void auditVertexElementMutation(AuditAction action, ElementMutation<Vertex> vertexElementMutation, Vertex vertex, String process,
                                           User user, Visibility visibility) {
        if (vertexElementMutation instanceof ExistingElementMutation) {
            Vertex oldVertex = (Vertex) ((ExistingElementMutation) vertexElementMutation).getElement();
            for (Property property : vertexElementMutation.getProperties()) {
                Object oldPropertyValue = oldVertex.getPropertyValue(property.getKey());
                Object newPropertyValue = property.getValue();
                checkNotNull(newPropertyValue, "new property (" + property + ") value cannot be null");
                if (!newPropertyValue.equals(oldPropertyValue) || !oldVertex.getVisibility().getVisibilityString().equals(property.getVisibility().getVisibilityString())) {
                    auditEntityProperty(action, oldVertex.getId(), property.getKey(), property.getName(), oldPropertyValue,
                            newPropertyValue, process, "", property.getMetadata(), user, visibility);
                }
            }
        } else {
            auditVertexCreate(vertex.getId(), process, "", user, visibility);
            for (Property property : vertexElementMutation.getProperties()) {
                Object newPropertyValue = property.getValue();
                checkNotNull(newPropertyValue, "new property (" + property + ") value cannot be null");
                auditEntityProperty(
                        action,
                        vertex.getId(),
                        property.getKey(),
                        property.getName(),
                        null,
                        newPropertyValue,
                        process,
                        "",
                        property.getMetadata(),
                        user,
                        visibility
                );
            }
        }
    }

    @Override
    public void auditEdgeElementMutation(AuditAction action, ElementMutation<Edge> edgeElementMutation, Edge edge, Vertex sourceVertex, Vertex destVertex, String process,
                                         User user, Visibility visibility) {
        if (edgeElementMutation instanceof ExistingElementMutation) {
            Edge oldEdge = (Edge) ((ExistingElementMutation) edgeElementMutation).getElement();
            for (Property property : edgeElementMutation.getProperties()) {
                Object oldPropertyValue = oldEdge.getPropertyValue(property.getKey());
                Object newPropertyValue = property.getValue();
                checkNotNull(newPropertyValue, "new property value cannot be null");
                if (!newPropertyValue.equals(oldPropertyValue)) {
                    auditRelationshipProperty(
                            action,
                            sourceVertex.getId(),
                            destVertex.getId(),
                            property.getKey(),
                            property.getName(),
                            oldPropertyValue,
                            newPropertyValue,
                            edge,
                            process,
                            "",
                            user,
                            visibility
                    );
                }
            }
        } else {
            auditRelationship(AuditAction.CREATE, sourceVertex, destVertex, edge, process, "", user, visibility);
            for (Property property : edgeElementMutation.getProperties()) {
                Object newPropertyValue = property.getValue();
                checkNotNull(newPropertyValue, "new property value cannot be null");
                auditRelationshipProperty(
                        action,
                        sourceVertex.getId(),
                        destVertex.getId(),
                        property.getKey(),
                        property.getName(),
                        null,
                        newPropertyValue,
                        edge,
                        process,
                        "",
                        user,
                        visibility
                );
            }
        }
    }

    @Override
    public void updateColumnVisibility(Audit audit, Visibility originalEdgeVisibility, String visibilityString, SimpleOrmContext context) {
        simpleOrmSession.alterVisibility(audit, orVisibility(originalEdgeVisibility).getVisibilityString(), visibilityString, context);
    }

    @Override
    public Iterable<Audit> findByIdStartsWith(String id, SimpleOrmContext simpleOrmContext) {
        return simpleOrmSession.findByIdStartsWith(Audit.class, id, simpleOrmContext);
    }

    private JSONObject jsonMetadata(Metadata metadata) {
        JSONObject json = new JSONObject();
        for (Metadata.Entry metadataEntry : metadata.entrySet()) {
            if (metadataEntry.getKey().equals(VisalloProperties.JUSTIFICATION.getPropertyName())) {
                PropertyJustificationMetadata propertyJustificationMetadata;
                Object metadataEntryValue = metadataEntry.getValue();
                if (metadataEntryValue instanceof PropertyJustificationMetadata) {
                    propertyJustificationMetadata = (PropertyJustificationMetadata) metadataEntryValue;
                } else {
                    propertyJustificationMetadata = new PropertyJustificationMetadata(new JSONObject(metadataEntryValue.toString()));
                }
                json.put(VisalloProperties.JUSTIFICATION.getPropertyName(), (propertyJustificationMetadata).toJson());
            } else {
                json.put(metadataEntry.getKey(), metadataEntry.getValue());
            }
        }
        return json;
    }

    private Visibility orVisibility(Visibility visibility) {
        String auditVisibilityLabel = configuration.get(Configuration.AUDIT_VISIBILITY_LABEL, null);
        if (auditVisibilityLabel != null && !visibility.toString().equals("")) {
            if (visibility.toString().equals(auditVisibilityLabel)) {
                return new Visibility(auditVisibilityLabel);
            }
            return new Visibility("(" + auditVisibilityLabel + "|" + visibility.toString() + ")");
        }
        return visibility;
    }

    private String checkAndConvertForDateType(String propertyName, Object value) {
        OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(propertyName);
        if (ontologyProperty != null && ontologyProperty.getDataType() == PropertyType.DATE) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyy");
            try {
                return String.valueOf(dateFormat.parse(value.toString()).getTime());
            } catch (ParseException e) {
                throw new RuntimeException("could not parse date");
            }
        }
        return null;
    }

    private Audit auditVertexCreate(String vertexId, String process, String comment, User user, Visibility visibility) {
        return auditVertex(AuditAction.CREATE, vertexId, process, comment, user, visibility);
    }
}
