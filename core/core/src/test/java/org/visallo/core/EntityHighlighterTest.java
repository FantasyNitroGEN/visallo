package org.visallo.core;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.vertexium.inmemory.InMemoryGraph;
import org.visallo.core.model.termMention.TermMentionBuilder;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.textHighlighting.OffsetItem;
import org.visallo.core.model.textHighlighting.VertexOffsetItem;
import org.visallo.core.security.DirectVisibilityTranslator;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EntityHighlighterTest {
    private static final String PROPERTY_KEY = "";
    private static final String PERSON_IRI = "http://visallo.org/test/person";
    private static final String LOCATION_IRI = "http://visallo.org/test/location";

    InMemoryGraph graph;

    @Mock
    private User user;
    private Authorizations authorizations;

    private Visibility visibility;

    private VisibilityTranslator visibilityTranslator = new DirectVisibilityTranslator();

    @Before
    public void setUp() {
        visibility = new Visibility("");
        graph = InMemoryGraph.create();
        authorizations = new InMemoryAuthorizations(TermMentionRepository.VISIBILITY_STRING);
    }

    @Test
    public void testGetHighlightedText() throws Exception {
        Vertex sourceVertex = graph.addVertex("1", visibility, authorizations);

        ArrayList<Vertex> terms = new ArrayList<>();
        terms.add(createTermMention(sourceVertex, "joe ferner", PERSON_IRI, 18, 28));
        terms.add(createTermMention(sourceVertex, "jeff kunkle", PERSON_IRI, 33, 44, "uniq1"));
        List<OffsetItem> termAndTermMetadata = new EntityHighlighter().convertTermMentionsToOffsetItems(terms, "", authorizations);
        String highlightedText = EntityHighlighter.getHighlightedText("Test highlight of Joe Ferner and Jeff Kunkle.", termAndTermMetadata);
        String expectedText = "Test highlight of <span class=\"vertex\" title=\"joe ferner\" data-info=\"{&quot;process&quot;:&quot;EntityHighlighterTest&quot;,&quot;id&quot;:&quot;TM_--18-28-EntityHighlighterTest&quot;,&quot;title&quot;:&quot;joe ferner&quot;,&quot;sandboxStatus&quot;:&quot;PRIVATE&quot;,&quot;start&quot;:18,&quot;sourceVertexId&quot;:&quot;1&quot;,&quot;http://visallo.org#conceptType&quot;:&quot;http://visallo.org/test/person&quot;,&quot;type&quot;:&quot;http://www.w3.org/2002/07/owl#Thing&quot;,&quot;end&quot;:28}\">Joe Ferner</span> and <span class=\"vertex\" title=\"jeff kunkle\" data-info=\"{&quot;process&quot;:&quot;uniq1&quot;,&quot;id&quot;:&quot;TM_--33-44-uniq1&quot;,&quot;title&quot;:&quot;jeff kunkle&quot;,&quot;sandboxStatus&quot;:&quot;PRIVATE&quot;,&quot;start&quot;:33,&quot;sourceVertexId&quot;:&quot;1&quot;,&quot;http://visallo.org#conceptType&quot;:&quot;http://visallo.org/test/person&quot;,&quot;type&quot;:&quot;http://www.w3.org/2002/07/owl#Thing&quot;,&quot;end&quot;:44}\">Jeff Kunkle</span>.";
        Elements expectedElements = Jsoup.parseBodyFragment(expectedText).getAllElements();
        Elements highlightedElements = Jsoup.parseBodyFragment(highlightedText).getAllElements();
        assertEquals(expectedElements.size(), highlightedElements.size());
        for (int i = 0; i < expectedElements.size(); i++) {
            Element expectedElement = expectedElements.get(i);
            Element highlightedElement = highlightedElements.get(i);
            assertEquals(expectedElement.attributes().size(), highlightedElement.attributes().size());
            for (Attribute expectedAttr : expectedElement.attributes()) {
                String expectedAttrValue = expectedAttr.getValue();
                String highlightedAttrValue = highlightedElement.attr(expectedAttr.getKey());
                if (expectedAttr.getKey().equals("data-info")) {
                    assertEquals(new JSONObject(expectedAttrValue).toString(), new JSONObject(highlightedAttrValue).toString());
                } else {
                    assertEquals(expectedAttrValue, highlightedAttrValue);
                }
            }
        }
    }

    private Vertex createTermMention(Vertex sourceVertex, String sign, String conceptIri, int start, int end) {
        return new TermMentionBuilder()
                .sourceVertex(sourceVertex)
                .propertyKey(PROPERTY_KEY)
                .conceptIri(conceptIri)
                .start(start)
                .end(end)
                .title(sign)
                .visibilityJson("")
                .process(getClass().getSimpleName())
                .save(graph, visibilityTranslator, authorizations);
    }

    private Vertex createTermMention(Vertex sourceVertex, String sign, String conceptIri, int start, int end, String process) {
        return new TermMentionBuilder()
                .sourceVertex(sourceVertex)
                .propertyKey(PROPERTY_KEY)
                .conceptIri(conceptIri)
                .start(start)
                .end(end)
                .title(sign)
                .visibilityJson("")
                .process(process)
                .save(graph, visibilityTranslator, authorizations);
    }

    public void testGetHighlightedTextOverlaps() throws Exception {
        Vertex sourceVertex = graph.addVertex("1", visibility, authorizations);

        ArrayList<Vertex> terms = new ArrayList<>();
        terms.add(createTermMention(sourceVertex, "joe ferner", PERSON_IRI, 18, 28));
        terms.add(createTermMention(sourceVertex, "jeff kunkle", PERSON_IRI, 18, 21));
        List<OffsetItem> termAndTermMetadata = new EntityHighlighter().convertTermMentionsToOffsetItems(terms, "", authorizations);
        String highlightText = EntityHighlighter.getHighlightedText("Test highlight of Joe Ferner.", termAndTermMetadata);
        assertEquals("Test highlight of <span class=\"entity person\" term-key=\"joe ferner\\x1Fee\\x1Fperson\"><span class=\"entity person\" term-key=\"joe\\x1Fee\\x1Fperson\">Joe</span> Ferner</span>.", highlightText);
    }

    @Test
    public void testGetHighlightedTextNestedEntity() throws Exception {
        String text = "This is a test sentence";
        List<OffsetItem> offsetItems = new ArrayList<>();

        OffsetItem mockEntity1 = mock(VertexOffsetItem.class);
        when(mockEntity1.getStart()).thenReturn(0l);
        when(mockEntity1.getEnd()).thenReturn(4l);
        when(mockEntity1.getResolvedToVertexId()).thenReturn("0");
        when(mockEntity1.getCssClasses()).thenReturn(asList(new String[]{"first"}));
        when(mockEntity1.shouldHighlight()).thenReturn(true);
        when(mockEntity1.getInfoJson()).thenReturn(new JSONObject("{\"data\":\"attribute\"}"));
        offsetItems.add(mockEntity1);

        OffsetItem mockEntity2 = mock(VertexOffsetItem.class);
        when(mockEntity2.getStart()).thenReturn(0l);
        when(mockEntity2.getEnd()).thenReturn(4l);
        when(mockEntity2.getResolvedToVertexId()).thenReturn("1");
        when(mockEntity2.getCssClasses()).thenReturn(asList(new String[]{"second"}));
        when(mockEntity2.shouldHighlight()).thenReturn(true);
        when(mockEntity2.getInfoJson()).thenReturn(new JSONObject("{\"data\":\"attribute\"}"));
        offsetItems.add(mockEntity2);

        OffsetItem mockEntity3 = mock(VertexOffsetItem.class);
        when(mockEntity3.getStart()).thenReturn(0l);
        when(mockEntity3.getEnd()).thenReturn(7l);
        when(mockEntity3.getCssClasses()).thenReturn(asList(new String[]{"third"}));
        when(mockEntity3.shouldHighlight()).thenReturn(true);
        when(mockEntity3.getInfoJson()).thenReturn(new JSONObject("{\"data\":\"attribute\"}"));
        offsetItems.add(mockEntity3);

        OffsetItem mockEntity4 = mock(VertexOffsetItem.class);
        when(mockEntity4.getStart()).thenReturn(5l);
        when(mockEntity4.getEnd()).thenReturn(9l);
        when(mockEntity4.getCssClasses()).thenReturn(asList(new String[]{"fourth"}));
        when(mockEntity4.shouldHighlight()).thenReturn(true);
        when(mockEntity4.getInfoJson()).thenReturn(new JSONObject("{\"data\":\"attribute\"}"));
        offsetItems.add(mockEntity4);

        OffsetItem mockEntity5 = mock(VertexOffsetItem.class);
        when(mockEntity5.getStart()).thenReturn(15l);
        when(mockEntity5.getEnd()).thenReturn(23l);
        when(mockEntity5.getCssClasses()).thenReturn(asList(new String[]{"fifth"}));
        when(mockEntity5.shouldHighlight()).thenReturn(true);
        when(mockEntity5.getInfoJson()).thenReturn(new JSONObject("{\"data\":\"attribute\"}"));
        offsetItems.add(mockEntity5);

        String highlightedText = EntityHighlighter.getHighlightedText(text, offsetItems);
        assertEquals("<span class=\"first\" data-info=\"{&quot;data&quot;:&quot;attribute&quot;}\">This</span> " +
                        "<span class=\"fourth\" data-info=\"{&quot;data&quot;:&quot;attribute&quot;}\">is a</span> test <span " +
                        "class=\"fifth\" data-info=\"{&quot;data&quot;:&quot;attribute&quot;}\">sentence</span>",
                highlightedText
        );
    }

    @Test
    public void testGetHighlightedTextWithAccentedCharacters() throws Exception {
        Vertex sourceVertex = graph.addVertex("1", visibility, authorizations);

        ArrayList<Vertex> terms = new ArrayList<>();
        terms.add(createTermMention(sourceVertex, "US", LOCATION_IRI, 48, 50));
        List<OffsetItem> termAndTermMetadata = new EntityHighlighter().convertTermMentionsToOffsetItems(terms, "", authorizations);
        String highlightText = EntityHighlighter.getHighlightedText("Ejército de Liberación Nacional® partnered with US on peace treaty", termAndTermMetadata);
        assertEquals("Ej&eacute;rcito de Liberaci&oacute;n Nacional&reg; partnered with <span class=\"vertex\" title=\"US\" data-info=\"{&quot;process&quot;:&quot;EntityHighlighterTest&quot;,&quot;id&quot;:&quot;TM_--48-50-EntityHighlighterTest&quot;,&quot;title&quot;:&quot;US&quot;,&quot;sandboxStatus&quot;:&quot;PRIVATE&quot;,&quot;start&quot;:48,&quot;sourceVertexId&quot;:&quot;1&quot;,&quot;http://visallo.org#conceptType&quot;:&quot;http://visallo.org/test/location&quot;,&quot;type&quot;:&quot;http://www.w3.org/2002/07/owl#Thing&quot;,&quot;end&quot;:50}\">US</span> on peace treaty",
                highlightText);
    }

    private List<String> asList(String[] strings) {
        List<String> results = new ArrayList<>();
        Collections.addAll(results, strings);
        return results;
    }
}
