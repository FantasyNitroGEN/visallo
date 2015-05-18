package org.visallo.core.model.audit;

import org.visallo.core.user.User;
import com.v5analytics.simpleorm.SimpleOrmContext;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;

public abstract class AuditRepository {
    public abstract Iterable<Audit> getAudits(String vertexId, String workspaceId, Authorizations authorizations);

    public abstract Audit auditVertex(
            AuditAction auditAction,
            String vertexId,
            String process,
            String comment,
            User user,
            Visibility visibility
    );

    public abstract Audit auditEntityProperty(
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
            Visibility visibility
    );

    public abstract Audit auditRelationship(
            AuditAction action,
            Vertex sourceVertex,
            Vertex destVertex,
            Edge edge,
            String process,
            String comment,
            User user,
            Visibility visibility
    );

    public abstract Audit auditRelationshipProperty(
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
    );

    public abstract Audit auditAnalyzedBy(AuditAction action, Vertex vertex, String process, User user, Visibility visibility);

    public abstract void auditVertexElementMutation(
            AuditAction action,
            ElementMutation<Vertex> vertexElementMutation,
            Vertex vertex,
            String process,
            User user,
            Visibility visibility
    );

    public abstract void auditEdgeElementMutation(
            AuditAction action,
            ElementMutation<Edge> edgeElementMutation,
            Edge edge,
            Vertex sourceVertex,
            Vertex destVertex,
            String process,
            User user,
            Visibility visibility
    );

    public abstract void updateColumnVisibility(
            Audit audit,
            Visibility originalEdgeVisibility,
            String visibilityString,
            SimpleOrmContext context
    );

    public abstract Iterable<Audit> findByIdStartsWith(String id, SimpleOrmContext simpleOrmContext);
}
