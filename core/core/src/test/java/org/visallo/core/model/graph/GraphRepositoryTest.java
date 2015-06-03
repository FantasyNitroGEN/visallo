package org.visallo.core.model.graph;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.*;
import org.vertexium.id.QueueIdGenerator;
import org.vertexium.inmemory.InMemoryGraph;
import org.vertexium.inmemory.InMemoryGraphConfiguration;
import org.vertexium.search.DefaultSearchIndex;
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
    private static final String WORKSPACE_ID = "testWorkspaceId";
    private static final String ENTITY_1_VERTEX_ID = "entity1Id";

    private GraphRepository graphRepository;
    private InMemoryGraph graph;

    @Mock
    private User user1;

    @Mock
    private TermMentionRepository termMentionRepository;

    private Authorizations defaultAuthorizations;

    @Before
    public void setup() throws Exception {
        InMemoryGraphConfiguration config = new InMemoryGraphConfiguration(new HashMap());
        QueueIdGenerator idGenerator = new QueueIdGenerator();
        VisibilityTranslator visibilityTranslator = new DirectVisibilityTranslator();
        graph = InMemoryGraph.create(config, idGenerator, new DefaultSearchIndex(config));
        defaultAuthorizations = graph.createAuthorizations();

        graphRepository = new GraphRepository(
                graph,
                visibilityTranslator,
                termMentionRepository
        );
    }

    @Test
    public void testSetWorkspaceOnlyChangePropertyTwice() {
        Vertex entity1Vertex = graph.prepareVertex(ENTITY_1_VERTEX_ID, new VisalloVisibility().getVisibility())
                .save(defaultAuthorizations);

        final Authorizations workspaceAuthorizations = graph.createAuthorizations(WORKSPACE_ID);

        setProperty(entity1Vertex, "newValue1", WORKSPACE_ID, workspaceAuthorizations);

        entity1Vertex = graph.getVertex(entity1Vertex.getId(), defaultAuthorizations);
        List<Property> properties = toList(entity1Vertex.getProperties());
        assertEquals(0, properties.size());

        entity1Vertex = graph.getVertex(entity1Vertex.getId(), workspaceAuthorizations);
        properties = toList(entity1Vertex.getProperties());
        assertEquals(1, properties.size());
        assertEquals("newValue1", properties.get(0).getValue());

        setProperty(entity1Vertex, "newValue2", WORKSPACE_ID, workspaceAuthorizations);

        entity1Vertex = graph.getVertex(entity1Vertex.getId(), defaultAuthorizations);
        properties = toList(entity1Vertex.getProperties());
        assertEquals(0, properties.size());

        entity1Vertex = graph.getVertex(entity1Vertex.getId(), workspaceAuthorizations);
        properties = toList(entity1Vertex.getProperties());
        assertEquals(1, properties.size());
        assertEquals("newValue2", properties.get(0).getValue());
    }

    @Test
    public void testSandboxPropertyChangesShouldUpdateSameProperty() {
        final Authorizations authorizations = graph.createAuthorizations("foo", "bar", "baz", WORKSPACE_ID);

        Vertex entity1Vertex = graph.prepareVertex(ENTITY_1_VERTEX_ID, new VisalloVisibility().getVisibility())
                .save(authorizations);

        // new property with visibility
        String propertyValue = "newValue1";
        setProperty(entity1Vertex, propertyValue, null, "foo", WORKSPACE_ID, authorizations);

        List<Property> properties = toList(entity1Vertex.getProperties());
        final Visibility fooVisibility = new VisalloVisibility(Visibility.and(ImmutableSet.of("foo", WORKSPACE_ID)))
                .getVisibility();
        assertEquals(1, properties.size());
        assertEquals(propertyValue, properties.get(0).getValue());
        assertEquals(fooVisibility, properties.get(0).getVisibility());

        // existing property, new visibility
        setProperty(entity1Vertex, propertyValue, "foo", "bar", WORKSPACE_ID, authorizations);

        properties = toList(entity1Vertex.getProperties());
        Visibility barVisibility = new VisalloVisibility(Visibility.and(ImmutableSet.of("bar", WORKSPACE_ID)))
                .getVisibility();
        assertEquals(1, properties.size());
        assertEquals(propertyValue, properties.get(0).getValue());
        assertEquals(barVisibility, properties.get(0).getVisibility());

        // existing property, new value
        propertyValue = "newValue2";
        setProperty(entity1Vertex, propertyValue, null, "bar", WORKSPACE_ID, authorizations);

        properties = toList(entity1Vertex.getProperties());
        assertEquals(1, properties.size());
        assertEquals(propertyValue, properties.get(0).getValue());
        assertEquals(barVisibility, properties.get(0).getVisibility());

        // existing property, new visibility,  new value
// TODO: needs fix to InMemoryGraph to be able to handle simultaneous change to both value and visibility
//        propertyValue = "newValue3";
//        setProperty(entity1Vertex, propertyValue, "bar", "baz", WORKSPACE_ID, authorizations);
//
//        properties = toList(entity1Vertex.getProperties());
//        final Visibility bazVisibility = new VisalloVisibility(Visibility.and(ImmutableSet.of("baz", WORKSPACE_ID)))
//                .getVisibility();
//        assertEquals(1, properties.size());
//        assertEquals(propertyValue, properties.get(0).getValue());
//        assertEquals(bazVisibility, properties.get(0).getVisibility());
    }

    private void setProperty(Vertex vertex, String value, String workspaceId, Authorizations workspaceAuthorizations) {
        setProperty(vertex, value, "", "", workspaceId, workspaceAuthorizations);
    }

    private void setProperty(Vertex vertex, String value, String oldVisibility, String newVisibility,
                             String workspaceId, Authorizations workspaceAuthorizations) {
        VisibilityAndElementMutation<Vertex> setPropertyResult = graphRepository.setProperty(
                vertex,
                "prop1",
                "key1",
                value,
                new Metadata(),
                oldVisibility,
                newVisibility,
                workspaceId,
                "I changed it",
                new ClientApiSourceInfo(),
                user1,
                workspaceAuthorizations
        );
        setPropertyResult.elementMutation.save(workspaceAuthorizations);
        graph.flush();
    }
}

