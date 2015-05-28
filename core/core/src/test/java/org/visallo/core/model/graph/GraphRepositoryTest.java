package org.visallo.core.model.graph;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.Metadata;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.id.QueueIdGenerator;
import org.vertexium.inmemory.InMemoryGraph;
import org.vertexium.inmemory.InMemoryGraphConfiguration;
import org.vertexium.search.DefaultSearchIndex;
import org.visallo.core.model.audit.AuditRepository;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.security.DirectVisibilityTranslator;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.vertexium.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class GraphRepositoryTest {
    private Vertex entity1Vertex;
    private GraphRepository graphRepository;
    private QueueIdGenerator idGenerator;
    private InMemoryGraph graph;

    @Mock
    private User user1;

    private VisibilityTranslator visibilityTranslator;

    @Mock
    private TermMentionRepository termMentionRepository;

    @Mock
    private AuditRepository auditRepository;

    private Authorizations defaultAuthorizations;

    @Before
    public void setup() throws Exception {
        InMemoryGraphConfiguration config = new InMemoryGraphConfiguration(new HashMap());
        idGenerator = new QueueIdGenerator();
        graph = InMemoryGraph.create(config, idGenerator, new DefaultSearchIndex(config));
        visibilityTranslator = new DirectVisibilityTranslator();
        defaultAuthorizations = graph.createAuthorizations();

        graphRepository = new GraphRepository(
                graph,
                visibilityTranslator,
                termMentionRepository,
                auditRepository
        );

        String entity1VertexId = "entity1Id";
        entity1Vertex = graph.prepareVertex(entity1VertexId, new VisalloVisibility().getVisibility())
                .save(defaultAuthorizations);
    }

    @Test
    public void testSetWorkspaceOnlyChangePropertyTwice() {
        String workspaceId = "testWorkspaceId";
        idGenerator.push(workspaceId);
        idGenerator.push(workspaceId + "_to_" + user1.getUserId());
        Authorizations workspaceAuthorizations = graph.createAuthorizations(workspaceId);

        VisibilityAndElementMutation<Vertex> setPropertyResult = graphRepository.setProperty(
                entity1Vertex,
                "prop1",
                "key1",
                "newValue1",
                new Metadata(),
                "",
                workspaceId,
                "I changed it",
                new ClientApiSourceInfo(),
                user1,
                workspaceAuthorizations
        );
        setPropertyResult.elementMutation.save(workspaceAuthorizations);
        graph.flush();

        entity1Vertex = graph.getVertex(entity1Vertex.getId(), defaultAuthorizations);
        List<Property> properties = toList(entity1Vertex.getProperties());
        assertEquals(0, properties.size());

        entity1Vertex = graph.getVertex(entity1Vertex.getId(), workspaceAuthorizations);
        properties = toList(entity1Vertex.getProperties());
        assertEquals(1, properties.size());
        assertEquals("newValue1", properties.get(0).getValue());

        setPropertyResult = graphRepository.setProperty(
                entity1Vertex,
                "prop1",
                "key1",
                "newValue2",
                new Metadata(),
                "",
                workspaceId,
                "I changed it",
                new ClientApiSourceInfo(),
                user1,
                workspaceAuthorizations
        );
        setPropertyResult.elementMutation.save(workspaceAuthorizations);
        graph.flush();

        entity1Vertex = graph.getVertex(entity1Vertex.getId(), defaultAuthorizations);
        properties = toList(entity1Vertex.getProperties());
        assertEquals(0, properties.size());

        entity1Vertex = graph.getVertex(entity1Vertex.getId(), workspaceAuthorizations);
        properties = toList(entity1Vertex.getProperties());
        assertEquals(1, properties.size());
        assertEquals("newValue2", properties.get(0).getValue());
    }
}
