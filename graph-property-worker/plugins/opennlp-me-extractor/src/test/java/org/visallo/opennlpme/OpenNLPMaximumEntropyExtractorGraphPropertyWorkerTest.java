package org.visallo.opennlpme;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Direction;
import org.vertexium.ElementBuilder;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerTestSetupBase;
import org.visallo.core.model.file.ClassPathFileSystemRepository;
import org.visallo.core.model.file.FileSystemRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.vertexium.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class OpenNLPMaximumEntropyExtractorGraphPropertyWorkerTest extends GraphPropertyWorkerTestSetupBase {
    private static final String TEST_FS_ROOT = "/testFsRoot";

    private static final String text = "This is a sentenc®, written by Bob Robértson, who curréntly makes 2 million "
            + "a year. If by 1:30, you don't know what you are doing, you should go watch CNN and see "
            + "what the latest is on the Benghazi nonsense. I'm 47% sure that this test will pass, but will it?";

    @Mock
    private FileSystemRepository fileSystemRepository;

    @Override
    protected GraphPropertyWorker createGraphPropertyWorker() {
        return new OpenNLPMaximumEntropyExtractorGraphPropertyWorker(new ClassPathFileSystemRepository(TEST_FS_ROOT));
    }

    @Test
    public void testEntityExtraction() throws Exception {
        VisibilityJson visibilityJson = new VisibilityJson();
        visibilityJson.setSource("");
        ElementBuilder<Vertex> vb = graph.prepareVertex("v1", new Visibility(""))
                .setProperty("text", "none", new Visibility(""));
        VisalloProperties.VISIBILITY_JSON.setProperty(vb, visibilityJson, new Visibility(""));
        Vertex vertex = vb.save(new InMemoryAuthorizations());

        GraphPropertyWorkData workData = new GraphPropertyWorkData(visibilityTranslator, vertex, vertex.getProperty("text"), null, null, Priority.NORMAL);
        worker.setVisibilityTranslator(visibilityTranslator);
        worker.execute(new ByteArrayInputStream(text.getBytes("UTF-8")), workData);

        List<Vertex> termMentions = toList(vertex.getVertices(Direction.OUT,
                VisalloProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, termMentionAuthorizations));

        assertEquals(3, termMentions.size());

        boolean foundBobRobertson = false;
        boolean foundBenghazi = false;
        boolean foundCnn = false;
        for (Vertex termMention : termMentions) {
            String title = VisalloProperties.TERM_MENTION_TITLE.getPropertyValue(termMention);

            long start = VisalloProperties.TERM_MENTION_START_OFFSET.getPropertyValue(termMention);
            long end = VisalloProperties.TERM_MENTION_END_OFFSET.getPropertyValue(termMention);
            String conceptType = VisalloProperties.TERM_MENTION_CONCEPT_TYPE.getPropertyValue(termMention);

            switch (title) {
                case "Bob Robértson":
                    foundBobRobertson = true;
                    assertEquals("http://visallo.org/test#person", conceptType);
                    assertEquals(31, start);
                    assertEquals(44, end);
                    break;
                case "Benghazi":
                    foundBenghazi = true;
                    assertEquals("http://visallo.org/test#location", conceptType);
                    assertEquals(189, start);
                    assertEquals(197, end);
                    break;
                case "CNN":
                    foundCnn = true;
                    assertEquals("http://visallo.org/test#organization", conceptType);
                    assertEquals(151, start);
                    assertEquals(154, end);
                    break;
            }
        }
        assertTrue("could not find bob robertson", foundBobRobertson);
        assertTrue("could not find benghazi", foundBenghazi);
        assertTrue("could not find cnn", foundCnn);
    }
}
