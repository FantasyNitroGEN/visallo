package org.visallo.web.routes.vertex;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Metadata;
import org.vertexium.Vertex;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.graph.VisibilityAndElementMutation;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.ACLProvider;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.VertexiumMetadataUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.JustificationText;
import org.visallo.web.routes.SetPropertyBase;
import org.visallo.web.util.VisibilityValidator;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class VertexSetProperty extends SetPropertyBase implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexSetProperty.class);

    private final OntologyRepository ontologyRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;
    private final GraphRepository graphRepository;
    private final ACLProvider aclProvider;
    private final boolean autoPublishComments;

    @Inject
    public VertexSetProperty(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final VisibilityTranslator visibilityTranslator,
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository,
            final GraphRepository graphRepository,
            final ACLProvider aclProvider,
            final Configuration configuration
    ) {
        super(graph, visibilityTranslator);
        this.ontologyRepository = ontologyRepository;
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
        this.graphRepository = graphRepository;
        this.aclProvider = aclProvider;
        this.autoPublishComments = configuration.getBoolean(
                Configuration.COMMENTS_AUTO_PUBLISH,
                Configuration.DEFAULT_COMMENTS_AUTO_PUBLISH
        );
    }

    @Handle
    public ClientApiElement handle(
            HttpServletRequest request,
            @Required(name = "graphVertexId") String graphVertexId,
            @Optional(name = "propertyKey") String propertyKey,
            @Required(name = "propertyName") String propertyName,
            @Optional(name = "value") String valueStr,
            @Optional(name = "value[]") String[] valuesStr,
            @Required(name = "visibilitySource") String visibilitySource,
            @Optional(name = "oldVisibilitySource") String oldVisibilitySource,
            @Optional(name = "sourceInfo") String sourceInfoString,
            @Optional(name = "metadata") String metadataString,
            @JustificationText String justificationText,
            @ActiveWorkspaceId String workspaceId,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        if (valueStr == null && valuesStr == null) {
            throw new VisalloException("Parameter: 'value' or 'value[]' is required in the request");
        }

        VisibilityValidator.validate(graph, visibilityTranslator, resourceBundle, visibilitySource, user, authorizations);
        checkRoutePath("vertex", propertyName, request);

        boolean isComment = isCommentProperty(propertyName);
        boolean autoPublish = isComment && autoPublishComments;
        if (autoPublish) {
            workspaceId = null;
        }

        if (propertyKey == null) {
            propertyKey = createPropertyKey(propertyName, graph);
        }

        Metadata metadata = VertexiumMetadataUtil.metadataStringToMap(metadataString, visibilityTranslator.getDefaultVisibility());
        ClientApiSourceInfo sourceInfo = ClientApiSourceInfo.fromString(sourceInfoString);
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);

        aclProvider.checkCanAddOrUpdateProperty(vertex, propertyKey, propertyName, user);

        List<SavePropertyResults> savePropertyResults = saveProperty(
                vertex,
                propertyKey,
                propertyName,
                valueStr,
                valuesStr,
                justificationText,
                oldVisibilitySource,
                visibilitySource,
                metadata,
                sourceInfo,
                user,
                workspaceId,
                authorizations
        );
        graph.flush();

        if (!autoPublish) {
            // add the vertex to the workspace so that the changes show up in the diff panel
            workspaceRepository.updateEntityOnWorkspace(workspaceId, vertex.getId(), null, null, user);
        }

        for (SavePropertyResults savePropertyResult : savePropertyResults) {
            workQueueRepository.pushGraphPropertyQueue(
                    vertex,
                    savePropertyResult.getPropertyKey(),
                    savePropertyResult.getPropertyName(),
                    workspaceId,
                    visibilitySource,
                    Priority.HIGH
            );
        }

        if (sourceInfo != null) {
            workQueueRepository.pushTextUpdated(sourceInfo.vertexId);
        }

        return ClientApiConverter.toClientApi(vertex, workspaceId, authorizations);
    }

    private List<SavePropertyResults> saveProperty(
            Vertex vertex,
            String propertyKey,
            String propertyName,
            String valueStr,
            String[] valuesStr,
            String justificationText,
            String oldVisibilitySource,
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
        if (isCommentProperty(propertyName)) {
            value = valueStr;
        } else {
            OntologyProperty property = ontologyRepository.getRequiredPropertyByIRI(propertyName);

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
                            vertex,
                            propertyKey,
                            dependentPropertyIri,
                            valuesStr[valuesIndex++],
                            null,
                            justificationText,
                            oldVisibilitySource,
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
                vertex,
                propertyName,
                propertyKey,
                value,
                metadata,
                oldVisibilitySource,
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
