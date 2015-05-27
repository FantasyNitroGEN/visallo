package org.visallo.mimeTypeOntologyMapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Metadata;
import org.vertexium.Vertex;
import org.vertexium.VertexBuilder;
import org.vertexium.Visibility;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.test.GraphPropertyWorkerTestBase;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MimeTypeOntologyMapperGraphPropertyWorkerTest extends GraphPropertyWorkerTestBase {
    private static final String TEST_AUDIO_IRI = "http://visallo.org/test#audio";
    private static final String TEST_DOCUMENT_IRI = "http://visallo.org/test#document";
    private Visibility visibility = new Visibility("");
    private GraphPropertyWorker gpw;

    @Mock
    private OntologyRepository ontologyRepository;

    @Mock
    private Concept audioConcept;

    @Mock
    private Concept documentConcept;

    @Before
    public void setUp() {
        when(ontologyRepository.getRequiredConceptByIntent(eq("audio"))).thenReturn(audioConcept);
        when(ontologyRepository.getRequiredConceptByIRI(eq(TEST_DOCUMENT_IRI))).thenReturn(documentConcept);
        when(audioConcept.getIRI()).thenReturn(TEST_AUDIO_IRI);
        when(documentConcept.getIRI()).thenReturn(TEST_DOCUMENT_IRI);

        gpw = new MimeTypeOntologyMapperGraphPropertyWorker();
        gpw.setOntologyRepository(ontologyRepository);
    }

    @Test
    public void testRegexMatch() {
        Metadata metadata = new Metadata();
        VertexBuilder m = getGraph().prepareVertex("v1", visibility);
        VisalloProperties.RAW.setProperty(m, StreamingPropertyValue.create("test"), metadata, visibility);
        VisalloProperties.MIME_TYPE.addPropertyValue(m, "k1", "audio/mp3", metadata, visibility);
        Vertex v1 = m.save(getGraphAuthorizations());
        getGraph().flush();

        run(gpw, getWorkerPrepareData(), v1, VisalloProperties.RAW.getProperty(v1), null);

        v1 = getGraph().getVertex("v1", getGraphAuthorizations());
        assertEquals(TEST_AUDIO_IRI, VisalloProperties.CONCEPT_TYPE.getPropertyValue(v1));
    }

    @Test
    public void testDefault() {
        Metadata metadata = new Metadata();
        VertexBuilder m = getGraph().prepareVertex("v1", visibility);
        VisalloProperties.RAW.setProperty(m, StreamingPropertyValue.create("test"), metadata, visibility);
        VisalloProperties.MIME_TYPE.addPropertyValue(m, "k1", "text/plain", metadata, visibility);
        Vertex v1 = m.save(getGraphAuthorizations());
        getGraph().flush();

        run(gpw, getWorkerPrepareData(), v1, VisalloProperties.RAW.getProperty(v1), null);

        v1 = getGraph().getVertex("v1", getGraphAuthorizations());
        assertEquals(TEST_DOCUMENT_IRI, VisalloProperties.CONCEPT_TYPE.getPropertyValue(v1));
    }

    @Override
    protected Map getConfigurationMap() {
        Map result = super.getConfigurationMap();
        result.put(MimeTypeOntologyMapperGraphPropertyWorker.class.getName() + ".mapping.audio.regex", "audio.*");
        result.put(MimeTypeOntologyMapperGraphPropertyWorker.class.getName() + ".mapping.audio.intent", "audio");
        result.put(MimeTypeOntologyMapperGraphPropertyWorker.class.getName() + ".mapping.default.iri", TEST_DOCUMENT_IRI);
        return result;
    }
}
