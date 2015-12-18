package org.visallo.core.util;

import org.vertexium.*;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.type.GeoPoint;
import org.vertexium.type.GeoRect;
import org.vertexium.util.IterableUtils;
import org.visallo.core.ingest.video.VideoFrameInfo;
import org.visallo.core.ingest.video.VideoPropertyHelper;
import org.visallo.core.model.properties.MediaVisalloProperties;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workspace.Dashboard;
import org.visallo.core.model.workspace.DashboardItem;
import org.visallo.web.clientapi.model.*;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClientApiConverter extends org.visallo.web.clientapi.util.ClientApiConverter {
    public static final EnumSet<FetchHint> SEARCH_FETCH_HINTS = EnumSet.of(FetchHint.PROPERTIES, FetchHint.PROPERTY_METADATA, FetchHint.IN_EDGE_LABELS, FetchHint.OUT_EDGE_LABELS);

    public static ClientApiTermMentionsResponse toTermMentionsResponse(Iterable<Vertex> termMentions, String workspaceId, Authorizations authorizations) {
        ClientApiTermMentionsResponse termMentionsResponse = new ClientApiTermMentionsResponse();
        for (ClientApiElement element : toClientApi(termMentions, workspaceId, authorizations)) {
            termMentionsResponse.getTermMentions().add(element);
        }
        return termMentionsResponse;
    }

    public static List<ClientApiElement> toClientApi(Iterable<? extends org.vertexium.Element> elements, String workspaceId, Authorizations authorizations) {
        List<ClientApiElement> clientApiElements = new ArrayList<>();
        for (org.vertexium.Element element : elements) {
            clientApiElements.add(toClientApi(element, workspaceId, authorizations));
        }
        return clientApiElements;
    }

    public static List<ClientApiVertex> toClientApiVertices(Iterable<? extends Vertex> vertices, String workspaceId, Authorizations authorizations) {
        List<ClientApiVertex> clientApiElements = new ArrayList<>();
        for (Vertex v : vertices) {
            clientApiElements.add(toClientApiVertex(v, workspaceId, authorizations));
        }
        return clientApiElements;
    }

    public static ClientApiElement toClientApi(org.vertexium.Element element, String workspaceId, Authorizations authorizations) {
        checkNotNull(element, "element cannot be null");
        if (element instanceof Vertex) {
            return toClientApiVertex((Vertex) element, workspaceId, authorizations);
        }
        if (element instanceof Edge) {
            return toClientApiEdge((Edge) element, workspaceId);
        }
        throw new RuntimeException("Unexpected element type: " + element.getClass().getName());
    }

    public static ClientApiVertex toClientApiVertex(Vertex vertex, String workspaceId, Authorizations authorizations) {
        return toClientApiVertex(vertex, workspaceId, null, authorizations);
    }

    /**
     * @param commonCount the number of vertices this vertex has in common with other vertices.
     */
    public static ClientApiVertex toClientApiVertex(Vertex vertex, String workspaceId, Integer commonCount, Authorizations authorizations) {
        checkNotNull(vertex, "vertex is required");
        ClientApiVertex v = new ClientApiVertex();

        List<String> vertexEdgeLabels = getVertexEdgeLabels(vertex, authorizations);
        if (vertexEdgeLabels != null) {
            v.getEdgeLabels().addAll(vertexEdgeLabels);
        }

        populateClientApiElement(v, vertex, workspaceId);
        v.setCommonCount(commonCount);
        return v;
    }

    private static List<String> getVertexEdgeLabels(Vertex vertex, Authorizations authorizations) {
        checkNotNull(vertex, "vertex is required");
        if (authorizations == null) {
            return null;
        }
        Iterable<String> edgeLabels = vertex.getEdgeLabels(Direction.BOTH, authorizations);
        return IterableUtils.toList(edgeLabels);
    }

    public static ClientApiEdge toClientApiEdge(Edge edge, String workspaceId) {
        ClientApiEdge e = new ClientApiEdge();
        populateClientApiEdge(e, edge, workspaceId);
        return e;
    }

    public static ClientApiEdgeWithVertexData toClientApiEdgeWithVertexData(Edge edge, Vertex source, Vertex target, String workspaceId, Authorizations authorizations) {
        checkNotNull(source, "source vertex is required");
        checkNotNull(target, "target vertex is required");
        ClientApiEdgeWithVertexData e = new ClientApiEdgeWithVertexData();
        e.setSource(toClientApiVertex(source, workspaceId, authorizations));
        e.setTarget(toClientApiVertex(target, workspaceId, authorizations));
        populateClientApiEdge(e, edge, workspaceId);
        return e;
    }

    public static void populateClientApiEdge(ClientApiEdge e, Edge edge, String workspaceId) {
        e.setLabel(edge.getLabel());
        e.setOutVertexId(edge.getVertexId(Direction.OUT));
        e.setInVertexId(edge.getVertexId(Direction.IN));

        populateClientApiElement(e, edge, workspaceId);
    }

    private static void populateClientApiElement(ClientApiElement clientApiElement, org.vertexium.Element element, String workspaceId) {
        clientApiElement.setId(element.getId());
        clientApiElement.getProperties().addAll(toClientApiProperties(element.getProperties(), workspaceId));
        clientApiElement.setSandboxStatus(SandboxStatusUtil.getSandboxStatus(element, workspaceId));

        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(element);
        if (visibilityJson != null) {
            clientApiElement.setVisibilitySource(visibilityJson.getSource());
        }

        if (clientApiElement instanceof ClientApiVertex) {
            ClientApiVertex clientApiVertex = (ClientApiVertex) clientApiElement;
            String conceptType = VisalloProperties.CONCEPT_TYPE.getPropertyValue(element, null);
            clientApiVertex.setConceptType(conceptType);
        }
    }

    public static List<ClientApiProperty> toClientApiProperties(Iterable<Property> properties, String workspaceId) {
        List<ClientApiProperty> clientApiProperties = new ArrayList<>();
        List<Property> propertiesList = IterableUtils.toList(properties);
        Collections.sort(propertiesList, new ConfidencePropertyComparator());
        SandboxStatus[] sandboxStatuses = SandboxStatusUtil.getPropertySandboxStatuses(propertiesList, workspaceId);
        for (int i = 0; i < propertiesList.size(); i++) {
            Property property = propertiesList.get(i);
            SandboxStatus sandboxStatus = sandboxStatuses[i];
            VideoFrameInfo videoFrameInfo;
            if ((videoFrameInfo = VideoPropertyHelper.getVideoFrameInfoFromProperty(property)) != null) {
                String textDescription = VisalloProperties.TEXT_DESCRIPTION_METADATA.getMetadataValueOrDefault(property.getMetadata(), null);
                addVideoFramePropertyToResults(clientApiProperties, videoFrameInfo.getPropertyKey(), textDescription, sandboxStatus);
            } else {
                ClientApiProperty clientApiProperty = toClientApiProperty(property);
                clientApiProperty.setSandboxStatus(sandboxStatus);
                clientApiProperties.add(clientApiProperty);
            }
        }
        return clientApiProperties;
    }

    public static ClientApiProperty toClientApiProperty(Property property) {
        ClientApiProperty clientApiProperty = new ClientApiProperty();
        clientApiProperty.setKey(property.getKey());
        clientApiProperty.setName(property.getName());

        Object propertyValue = property.getValue();
        if (propertyValue instanceof StreamingPropertyValue) {
            clientApiProperty.setStreamingPropertyValue(true);
        } else {
            clientApiProperty.setValue(toClientApiValue(propertyValue));
        }

        for (Metadata.Entry entry : property.getMetadata().entrySet()) {
            clientApiProperty.getMetadata().put(entry.getKey(), toClientApiValue(entry.getValue()));
        }

        return clientApiProperty;
    }

    private static void addVideoFramePropertyToResults(List<ClientApiProperty> clientApiProperties, String propertyKey, String textDescription, SandboxStatus sandboxStatus) {
        ClientApiProperty clientApiProperty = findProperty(clientApiProperties, MediaVisalloProperties.VIDEO_TRANSCRIPT.getPropertyName(), propertyKey);
        if (clientApiProperty == null) {
            clientApiProperty = new ClientApiProperty();
            clientApiProperty.setKey(propertyKey);
            clientApiProperty.setName(MediaVisalloProperties.VIDEO_TRANSCRIPT.getPropertyName());
            clientApiProperty.setSandboxStatus(sandboxStatus);
            clientApiProperty.getMetadata().put(VisalloProperties.TEXT_DESCRIPTION_METADATA.getMetadataKey(), textDescription);
            clientApiProperty.setStreamingPropertyValue(true);
            clientApiProperties.add(clientApiProperty);
        }
    }

    private static ClientApiProperty findProperty(List<ClientApiProperty> clientApiProperties, String propertyName, String propertyKey) {
        for (ClientApiProperty property : clientApiProperties) {
            if (property.getName().equals(propertyName) && property.getKey().equals(propertyKey)) {
                return property;
            }
        }
        return null;
    }

    public static ClientApiHistoricalPropertyValues toClientApi(Iterable<HistoricalPropertyValue> historicalPropertyValues) {
        ClientApiHistoricalPropertyValues result = new ClientApiHistoricalPropertyValues();
        for (HistoricalPropertyValue historicalPropertyValue : historicalPropertyValues) {
            result.values.add(toClientApi(historicalPropertyValue));
        }
        return result;
    }

    public static ClientApiHistoricalPropertyValues.Value toClientApi(HistoricalPropertyValue hpv) {
        ClientApiHistoricalPropertyValues.Value result = new ClientApiHistoricalPropertyValues.Value();
        result.timestamp = hpv.getTimestamp();
        for (Metadata.Entry entry : hpv.getMetadata().entrySet()) {
            result.metadata.put(entry.getKey(), toClientApiValue(entry.getValue()));
        }
        result.value = toClientApiValue(hpv.getValue());
        return result;
    }

    public static ClientApiDashboards toClientApiDashboards(Collection<Dashboard> dashboards) {
        ClientApiDashboards result = new ClientApiDashboards();
        for (Dashboard dashboard : dashboards) {
            result.dashboards.add(toClientApiDashboard(dashboard));
        }
        return result;
    }

    public static ClientApiDashboard toClientApiDashboard(Dashboard dashboard) {
        ClientApiDashboard result = new ClientApiDashboard();
        result.id = dashboard.getId();
        result.workspaceId = dashboard.getWorkspaceId();
        result.title = dashboard.getTitle();
        for (DashboardItem dashboardItem : dashboard.getItems()) {
            result.items.add(toClientApiDashboardItem(dashboardItem));
        }
        return result;
    }

    private static ClientApiDashboard.Item toClientApiDashboardItem(DashboardItem dashboardItem) {
        ClientApiDashboard.Item result = new ClientApiDashboard.Item();
        result.id = dashboardItem.getId();
        result.extensionId = dashboardItem.getExtensionId();
        result.title = dashboardItem.getTitle();
        result.configuration = dashboardItem.getConfiguration();
        return result;
    }

    public static ClientApiGeoPoint toClientApiGeoPoint(GeoPoint geoPoint) {
        return new ClientApiGeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude());
    }

    public static ClientApiGeoRect toClientApiGeoRect(GeoRect rect) {
        return new ClientApiGeoRect(
                toClientApiGeoPoint(rect.getNorthWest()),
                toClientApiGeoPoint(rect.getSouthEast())
        );
    }
}
