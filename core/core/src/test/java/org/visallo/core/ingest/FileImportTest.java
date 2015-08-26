package org.visallo.core.ingest;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.inmemory.InMemoryGraph;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.WorkQueueNames;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.types.IntegerVisalloProperty;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.DirectVisibilityTranslator;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiImportProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.vertexium.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class FileImportTest {
    public static final String PROP1_NAME = "http://visallo.org#prop1";
    private FileImport fileImport;

    private Graph graph;

    private VisibilityTranslator visibilityTranslator;

    @Mock
    private WorkQueueRepository workQueueRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkQueueNames workQueueNames;

    @Mock
    private OntologyRepository ontologyRepository;

    @Mock
    private Configuration configuration;

    @Mock
    User user;

    Authorizations authorizations;

    @Mock
    Workspace workspace;

    @Mock
    OntologyProperty ontologyProperty;

    @Before
    public void setup() {
        graph = InMemoryGraph.create();
        visibilityTranslator = new DirectVisibilityTranslator();
        authorizations = graph.createAuthorizations();

        when(ontologyRepository.getRequiredPropertyByIntent(PROP1_NAME)).thenReturn(ontologyProperty);
        when(ontologyProperty.getVisalloProperty()).thenReturn(new IntegerVisalloProperty(PROP1_NAME));

        fileImport = new FileImport(
                visibilityTranslator,
                graph,
                workQueueRepository,
                workspaceRepository,
                workQueueNames,
                ontologyRepository,
                configuration
        ) {
            @Override
            protected List<PostFileImportHandler> getPostFileImportHandlers() {
                return new ArrayList<>();
            }

            @Override
            protected List<FileImportSupportingFileHandler> getFileImportSupportingFileHandlers() {
                return new ArrayList<>();
            }
        };
    }

    @Test
    public void testImportVertices() throws Exception {
        File testFile = File.createTempFile("test", "test");
        testFile.deleteOnExit();

        FileUtils.writeStringToFile(testFile, "<html><head><title>Test HTML</title><body>Hello Test</body></html>");

        List<FileImport.FileOptions> files = new ArrayList<>();
        FileImport.FileOptions file = new FileImport.FileOptions();
        file.setConceptId("http://visallo.org/testConcept");
        file.setFile(testFile);
        ClientApiImportProperty[] properties = new ClientApiImportProperty[1];
        properties[0] = new ClientApiImportProperty();
        properties[0].setKey("k1");
        properties[0].setName(PROP1_NAME);
        properties[0].setVisibilitySource("");
        properties[0].setValue("42");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("m1", "v1");
        properties[0].setMetadata(metadata);
        file.setProperties(properties);
        file.setVisibilitySource("");
        files.add(file);
        Priority priority = Priority.NORMAL;
        List<Vertex> results = fileImport.importVertices(workspace, files, priority, user, authorizations);
        assertEquals(1, results.size());

        Vertex v1 = graph.getVertex(results.get(0).getId(), authorizations);
        List<Property> foundProperties = toList(v1.getProperties());
        assertEquals(6, foundProperties.size());
        for (int i = 0; i < 6; i++) {
            Property foundProperty = foundProperties.get(i);
            if (foundProperty.getName().equals(PROP1_NAME)) {
                assertEquals("k1", foundProperty.getKey());
                assertEquals(42, foundProperty.getValue());
                assertEquals(1, foundProperty.getMetadata().entrySet().size());
                assertEquals("v1", foundProperty.getMetadata().getValue("m1"));
            }
        }
    }
}