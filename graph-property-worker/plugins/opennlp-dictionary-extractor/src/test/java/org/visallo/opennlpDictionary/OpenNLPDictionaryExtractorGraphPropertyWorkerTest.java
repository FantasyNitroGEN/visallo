package org.visallo.opennlpDictionary;

import com.google.common.collect.ImmutableMap;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerTestSetupBase;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.opennlpDictionary.model.DictionaryEntryRepository;
import org.visallo.web.clientapi.model.VisibilityJson;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.namefind.DictionaryNameFinder;
import opennlp.tools.namefind.TokenNameFinder;
import opennlp.tools.util.StringList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.ElementBuilder;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.vertexium.inmemory.InMemoryAuthorizations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.vertexium.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class OpenNLPDictionaryExtractorGraphPropertyWorkerTest extends GraphPropertyWorkerTestSetupBase {
    private static final String RESOURCE_CONFIG_DIR = "/fs/conf/opennlp";

    @Mock
    private DictionaryEntryRepository dictionaryEntryRepository;

    @Override
    protected GraphPropertyWorker createGraphPropertyWorker() {
        final List<TokenNameFinder> finders = loadFinders();
        OpenNLPDictionaryExtractorGraphPropertyWorker worker = new OpenNLPDictionaryExtractorGraphPropertyWorker() {
            @Override
            protected List<TokenNameFinder> loadFinders() throws IOException {
                return finders;
            }
        };
        worker.setDictionaryEntryRepository(dictionaryEntryRepository);
        return worker;
    }

    @Override
    protected Map<String, String> getAdditionalConfiguration() {
        return ImmutableMap.of(
                OpenNLPDictionaryExtractorGraphPropertyWorker.PATH_PREFIX_CONFIG,
                "file:///" + getClass().getResource(RESOURCE_CONFIG_DIR).getFile());
    }

    @Test
    public void testEntityExtraction() throws Exception {
        VisibilityJson visibilityJson = new VisibilityJson();
        visibilityJson.setSource("");
        ElementBuilder<Vertex> vb = graph.prepareVertex("v1", new Visibility(""))
                .setProperty("text", "none", new Visibility(""));
        VisalloProperties.VISIBILITY_JSON.setProperty(vb, visibilityJson, new Visibility(""));
        Vertex vertex = vb.save(new InMemoryAuthorizations());
        graph.flush();

        GraphPropertyWorkData workData = new GraphPropertyWorkData(
                visibilityTranslator,
                vertex,
                vertex.getProperty("text"),
                WORKSPACE_ID,
                VISIBILITY_SOURCE,
                Priority.NORMAL
        );
        String text = "This is a sentence that is going to tell you about a guy named "
                + "Bob Robertson who lives in Boston, MA and works for a company called V5 Analytics";
        worker.execute(new ByteArrayInputStream(text.getBytes()), workData);

        List<Vertex> termMentions = toList(termMentionRepository.findBySourceGraphVertex(vertex.getId(),
                termMentionAuthorizations));

        assertEquals(3, termMentions.size());

        boolean found = false;
        for (Vertex term : termMentions) {
            String title = VisalloProperties.TERM_MENTION_TITLE.getPropertyValue(term);
            if (title.equals("Bob Robertson")) {
                found = true;
                assertEquals(63, VisalloProperties.TERM_MENTION_START_OFFSET.getPropertyValue(term, 0));
                assertEquals(76, VisalloProperties.TERM_MENTION_END_OFFSET.getPropertyValue(term, 0));
                break;
            }
        }
        assertTrue("Expected name not found!", found);

        ArrayList<String> signs = new ArrayList<>();
        for (Vertex term : termMentions) {
            String title = VisalloProperties.TERM_MENTION_TITLE.getPropertyValue(term);
            signs.add(title);
        }

        assertTrue("Bob Robertson not found", signs.contains("Bob Robertson"));
        assertTrue("V5 Analytics not found", signs.contains("V5 Analytics"));
        assertTrue("Boston , MA not found", signs.contains("Boston , MA"));
    }

    private List<TokenNameFinder> loadFinders() {
        List<TokenNameFinder> finders = new ArrayList<>();
        Dictionary people = new Dictionary();
        people.put(new StringList("Bob Robertson".split(" ")));
        finders.add(new DictionaryNameFinder(people, "person"));

        Dictionary locations = new Dictionary();
        locations.put(new StringList("Boston , MA".split(" ")));
        finders.add(new DictionaryNameFinder(locations, "location"));

        Dictionary organizations = new Dictionary();
        organizations.put(new StringList("V5 Analytics".split(" ")));
        finders.add(new DictionaryNameFinder(organizations, "organization"));

        return finders;
    }
}
