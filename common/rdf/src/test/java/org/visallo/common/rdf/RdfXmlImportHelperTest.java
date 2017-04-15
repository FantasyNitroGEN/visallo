package org.visallo.common.rdf;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryGraph;
import org.vertexium.util.IterableUtils;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.DirectVisibilityTranslator;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RdfXmlImportHelperTest {
    private RdfXmlImportHelper rdfXmlImportHelper;
    private InMemoryGraph graph;
    private String defaultVisibilitySource;
    private String sourceFileName;
    private Authorizations authorizations;

    @Mock
    private WorkQueueRepository workQueueRepository;

    @Mock
    private User user;

    @Mock
    private OntologyRepository ontologyRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Before
    public void setUp() {
        graph = InMemoryGraph.create();
        authorizations = graph.createAuthorizations("A");
        VisibilityTranslator visibilityTranslator = new DirectVisibilityTranslator();

        when(user.getUserId()).thenReturn("user1");
        rdfXmlImportHelper = new RdfXmlImportHelper(graph, workQueueRepository, ontologyRepository, workspaceRepository, visibilityTranslator);
        defaultVisibilitySource = "";
        sourceFileName = "test.rdf";
        graph.flush();
    }

    @Test
    public void testImportVertexHasMetadata () throws IOException {
        File file = new File(RdfXmlImportHelperTest.class.getResource(sourceFileName).getFile());
        rdfXmlImportHelper.importRdfXml(file, null, Priority.HIGH, defaultVisibilitySource, user, authorizations);
        Vertex visallo = graph.getVertex("COMPANY_visallo", authorizations);
        assertEquals (5, IterableUtils.count(visallo.getProperties()));
        assertEquals("http://visallo.org/test#company", VisalloProperties.CONCEPT_TYPE.getPropertyValue(visallo, null));
        assertEquals("user1", VisalloProperties.MODIFIED_BY.getPropertyValue(visallo, null));
        assertNotNull(VisalloProperties.MODIFIED_DATE.getPropertyValue(visallo));

        VisibilityJson visibilityJson = new VisibilityJson(defaultVisibilitySource);
        assertEquals(visibilityJson, VisalloProperties.VISIBILITY_JSON.getPropertyValue(visallo, null));

        Property property = VisalloProperties.TITLE.getProperty(visallo, "RdfXmlImportHelper");
        assertNotNull(property);
        assertEquals("Visallo, LLC", property.getValue());
        Metadata metadata = property.getMetadata();
        assertEquals(3, metadata.entrySet().size());
        assertTrue(metadata.containsKey(VisalloProperties.VISIBILITY_JSON_METADATA.getMetadataKey()));
        assertTrue(metadata.containsKey(VisalloProperties.MODIFIED_BY_METADATA.getMetadataKey()));
        assertTrue(metadata.containsKey(VisalloProperties.MODIFIED_DATE_METADATA.getMetadataKey()));

        Edge e1 = graph.getEdge("PERSON_susan_http://visallo.org#worksFor_COMPANY_visallo", authorizations);
        assertEquals (3, IterableUtils.count(e1.getProperties()));
        assertEquals("user1", VisalloProperties.MODIFIED_BY.getPropertyValue(e1, null));
        assertNotNull(VisalloProperties.MODIFIED_DATE.getPropertyValue(e1));
        assertEquals(visibilityJson, VisalloProperties.VISIBILITY_JSON.getPropertyValue(e1, null));
    }
}
