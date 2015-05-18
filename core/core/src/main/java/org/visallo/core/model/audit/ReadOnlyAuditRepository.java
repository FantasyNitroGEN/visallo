package org.visallo.core.model.audit;

import com.google.inject.Singleton;
import org.visallo.core.user.User;
import com.v5analytics.simpleorm.SimpleOrmContext;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;

import java.util.ArrayList;

@Singleton
public class ReadOnlyAuditRepository extends AuditRepository {
    @Override
    public Iterable<Audit> getAudits(String vertexId, String workspaceId, Authorizations authorizations) {
        return new ArrayList<>();
    }

    @Override
    public Iterable<Audit> findByIdStartsWith(String id, SimpleOrmContext simpleOrmContext) {
        return new ArrayList<>();
    }

    @Override
    public Audit auditVertex(
            AuditAction auditAction,
            String vertexId,
            String process,
            String comment,
            User user,
            Visibility visibility
    ) {
        throw new RuntimeException("not supported");
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
            Visibility visibility
    ) {
        throw new RuntimeException("not supported");
    }

    @Override
    public Audit auditRelationship(AuditAction action, Vertex sourceVertex, Vertex destVertex, Edge edge, String process, String comment, User user, Visibility visibility) {
        throw new RuntimeException("not supported");
    }

    @Override
    public Audit auditRelationshipProperty(AuditAction action, String sourceId, String destId, String propertyKey,
                                           String propertyName, Object oldValue, Object newValue, Edge edge, String process, String comment, User user, Visibility visibility) {
        throw new RuntimeException("not supported");
    }

    @Override
    public Audit auditAnalyzedBy(AuditAction action, Vertex vertex, String process, User user, Visibility visibility) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void auditVertexElementMutation(AuditAction action, ElementMutation<Vertex> vertexElementMutation, Vertex vertex, String process, User user, Visibility visibility) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void auditEdgeElementMutation(AuditAction action, ElementMutation<Edge> edgeElementMutation, Edge edge, Vertex sourceVertex, Vertex destVertex, String process, User user, Visibility visibility) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void updateColumnVisibility(Audit audit, Visibility originalEdgeVisibility, String visibilityString, SimpleOrmContext context) {
        throw new RuntimeException("not supported");
    }
}
