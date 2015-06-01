package org.visallo.web.routes.vertex;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.*;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.graph.VisibilityAndElementMutation;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.VertexiumMetadataUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.WebConfiguration;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VertexSetProperty extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexSetProperty.class);

    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;
    private final GraphRepository graphRepository;

    @Inject
    public VertexSetProperty(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final VisibilityTranslator visibilityTranslator,
            final UserRepository userRepository,
            final Configuration configuration,
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository,
            final GraphRepository graphRepository
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
        this.graphRepository = graphRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        final String propertyName = getRequiredParameter(request, "propertyName");
        String propertyKey = getOptionalParameter(request, "propertyKey");
        final String valueStr = getOptionalParameter(request, "value");
        final String[] valuesStr = getOptionalParameterArray(request, "value[]");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");
        boolean isComment = VisalloProperties.COMMENT.getPropertyName().equals(propertyName);
        final String justificationText = isJustificationRequired(isComment) ? getRequiredParameter(request, "justificationText") : getOptionalParameter(request, "justificationText");
        final String sourceInfo = getOptionalParameter(request, "sourceInfo");
        final String metadataString = getOptionalParameter(request, "metadata");
        User user = getUser(request);
        String workspaceId = getActiveWorkspaceId(request);
        Authorizations authorizations = getAuthorizations(request, user);

        if (valueStr == null && valuesStr == null) {
            throw new VisalloException("Parameter: 'value' or 'value[]' is required in the request");
        }

        if (isComment && propertyKey == null) {
            propertyKey = createCommentPropertyKey();
        }

        if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", getString(request, "visibility.invalid"));
            chain.next(request, response);
            return;
        }

        if (propertyName.equals(VisalloProperties.COMMENT.getPropertyName()) && request.getPathInfo().equals("/vertex/property")) {
            throw new VisalloException("Use /vertex/comment to save comment properties");
        } else if (request.getPathInfo().equals("/vertex/comment") && !propertyName.equals(VisalloProperties.COMMENT.getPropertyName())) {
            throw new VisalloException("Use /vertex/property to save non-comment properties");
        }

        respondWithClientApiObject(response, handle(
                graphVertexId,
                propertyName,
                propertyKey,
                valueStr,
                valuesStr,
                justificationText,
                sourceInfo,
                metadataString,
                visibilitySource,
                user,
                workspaceId,
                authorizations));
    }

    private boolean isJustificationRequired(boolean isComment) {
        if (isComment) {
            return false;
        }

        return WebConfiguration.justificationRequired(getConfiguration());
    }

    private String createCommentPropertyKey() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return dateFormat.format(new Date());
    }

    private ClientApiElement handle(
            String graphVertexId,
            String propertyName,
            String propertyKey,
            String valueStr,
            String[] valuesStr,
            String justificationText,
            String sourceInfoString,
            String metadataString,
            String visibilitySource,
            User user,
            String workspaceId,
            Authorizations authorizations
    ) {
        if (propertyKey == null) {
            propertyKey = this.graph.getIdGenerator().nextId();
        }

        Metadata metadata = VertexiumMetadataUtil.metadataStringToMap(metadataString, this.visibilityTranslator.getDefaultVisibility());
        ClientApiSourceInfo sourceInfo = ClientApiSourceInfo.fromString(sourceInfoString);
        Vertex graphVertex = graph.getVertex(graphVertexId, authorizations);
        List<SavePropertyResults> savePropertyResults = saveProperty(
                graphVertex,
                propertyKey,
                propertyName,
                valueStr,
                valuesStr,
                justificationText,
                visibilitySource,
                metadata,
                sourceInfo,
                user,
                workspaceId,
                authorizations
        );
        graph.flush();

        Workspace workspace = workspaceRepository.findById(workspaceId, user);

        this.workspaceRepository.updateEntityOnWorkspace(workspace, graphVertex.getId(), null, null, user);

        for (SavePropertyResults savePropertyResult : savePropertyResults) {
            this.workQueueRepository.pushGraphPropertyQueue(
                    graphVertex,
                    savePropertyResult.getPropertyKey(),
                    savePropertyResult.getPropertyName(),
                    workspaceId,
                    visibilitySource,
                    Priority.HIGH
            );
        }

        if (sourceInfo != null) {
            this.workQueueRepository.pushTextUpdated(sourceInfo.vertexId);
        }

        return ClientApiConverter.toClientApi(graphVertex, workspaceId, authorizations);
    }

    private List<SavePropertyResults> saveProperty(
            Vertex graphVertex,
            String propertyKey,
            String propertyName,
            String valueStr,
            String[] valuesStr,
            String justificationText,
            String visibilitySource,
            Metadata metadata,
            ClientApiSourceInfo sourceInfo,
            User user,
            String workspaceId,
            Authorizations authorizations
    ) {
        if (valueStr == null && valuesStr != null && valuesStr.length == 1) {
            valueStr = valuesStr[0];
        }
        if (valuesStr == null && valueStr != null) {
            valuesStr = new String[1];
            valuesStr[0] = valueStr;
        }

        Object value;
        if (propertyName.equals(VisalloProperties.COMMENT.getPropertyName())) {
            value = valueStr;
        } else {
            OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyName);
            if (property == null) {
                throw new RuntimeException("Could not find property: " + propertyName);
            }

            if (property.hasDependentPropertyIris()) {
                if (valuesStr == null) {
                    throw new VisalloException("properties with dependent properties must contain a value");
                }
                if (property.getDependentPropertyIris().size() != valuesStr.length) {
                    throw new VisalloException("properties with dependent properties must contain the same number of values. expected " + property.getDependentPropertyIris().size() + " found " + valuesStr.length);
                }

                int valuesIndex = 0;
                List<SavePropertyResults> results = new ArrayList<>();
                for (String dependentPropertyIri : property.getDependentPropertyIris()) {
                    results.addAll(saveProperty(
                            graphVertex,
                            propertyKey,
                            dependentPropertyIri,
                            valuesStr[valuesIndex++],
                            null,
                            justificationText,
                            visibilitySource,
                            metadata,
                            sourceInfo,
                            user,
                            workspaceId,
                            authorizations
                    ));
                }
                return results;
            } else {
                if (valuesStr != null && valuesStr.length > 1) {
                    throw new VisalloException("properties without dependent properties must not contain more than one value.");
                }
                if (valueStr == null) {
                    throw new VisalloException("properties without dependent properties must have a value");
                }
                try {
                    value = property.convertString(valueStr);
                } catch (Exception ex) {
                    LOGGER.warn(String.format("Validation error propertyName: %s, valueStr: %s", propertyName, valueStr), ex);
                    throw new VisalloException(ex.getMessage(), ex);
                }
            }
        }

        VisibilityAndElementMutation<Vertex> setPropertyResult = graphRepository.setProperty(
                graphVertex,
                propertyName,
                propertyKey,
                value,
                metadata,
                visibilitySource,
                workspaceId,
                justificationText,
                sourceInfo,
                user,
                authorizations
        );
        Vertex save = setPropertyResult.elementMutation.save(authorizations);
        return Lists.newArrayList(new SavePropertyResults(save, propertyKey, propertyName));
    }

    private static class SavePropertyResults {
        private final Vertex vertex;
        private final String propertyKey;
        private final String propertyName;

        public SavePropertyResults(Vertex vertex, String propertyKey, String propertyName) {
            this.vertex = vertex;
            this.propertyKey = propertyKey;
            this.propertyName = propertyName;
        }

        public Vertex getVertex() {
            return vertex;
        }

        public String getPropertyKey() {
            return propertyKey;
        }

        public String getPropertyName() {
            return propertyName;
        }
    }
}
