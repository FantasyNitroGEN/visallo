package org.visallo.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.vertexium.inmemory.InMemoryGraph;
import org.visallo.core.model.properties.VisalloProperties;
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
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

        when(user.getUserId()).thenReturn("USER123");
    }

    @Test
    public void testGetHighlightedText() throws Exception {
        Vertex outVertex = graph.addVertex("1", visibility, authorizations);

        ArrayList<Vertex> terms = new ArrayList<>();
        terms.add(createTermMention(outVertex, "joe ferner", PERSON_IRI, 18, 28));
        terms.add(createTermMention(outVertex, "jeff kunkle", PERSON_IRI, 33, 44, "uniq1"));
        List<OffsetItem> termAndTermMetadata = new EntityHighlighter().convertTermMentionsToOffsetItems(terms, "", authorizations);
        String highlightedText = EntityHighlighter.getHighlightedText("Test highlight of Joe Ferner and Jeff Kunkle.", termAndTermMetadata);
        String expectedText = "Test highlight of <span class=\"vertex\" title=\"joe ferner\" data-info=\"{&quot;process&quot;:&quot;EntityHighlighterTest&quot;,&quot;id&quot;:&quot;TM_1--18-28-EntityHighlighterTest&quot;,&quot;title&quot;:&quot;joe ferner&quot;,&quot;sandboxStatus&quot;:&quot;PRIVATE&quot;,&quot;start&quot;:18,&quot;outVertexId&quot;:&quot;1&quot;,&quot;http://visallo.org#conceptType&quot;:&quot;http://visallo.org/test/person&quot;,&quot;type&quot;:&quot;http://www.w3.org/2002/07/owl#Thing&quot;,&quot;end&quot;:28}\">Joe Ferner</span> and <span class=\"vertex\" title=\"jeff kunkle\" data-info=\"{&quot;process&quot;:&quot;uniq1&quot;,&quot;id&quot;:&quot;TM_1--33-44-uniq1&quot;,&quot;title&quot;:&quot;jeff kunkle&quot;,&quot;sandboxStatus&quot;:&quot;PRIVATE&quot;,&quot;start&quot;:33,&quot;outVertexId&quot;:&quot;1&quot;,&quot;http://visallo.org#conceptType&quot;:&quot;http://visallo.org/test/person&quot;,&quot;type&quot;:&quot;http://www.w3.org/2002/07/owl#Thing&quot;,&quot;end&quot;:44}\">Jeff Kunkle</span>.";
        assertHighlightedTextSame(expectedText, highlightedText);
    }

    private void assertHighlightedTextSame(String expectedText, String highlightedText) {
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
                    assertJSONEquals(new JSONObject(expectedAttrValue), new JSONObject(highlightedAttrValue));
                } else {
                    assertEquals(expectedAttrValue, highlightedAttrValue);
                }
            }
        }
    }

    private void assertJSONEquals(JSONObject obj1, JSONObject obj2) {
        Set<String> obj1Set = setToString(obj1.keySet());
        Set<String> obj2Set = setToString(obj2.keySet());

        Sets.SetView<String> difference = Sets.difference(obj1Set, obj2Set);

        assertTrue(difference.isEmpty());

        for (String obj1Key : obj1Set) {
            Object val = obj1.get(obj1Key);
            if (val instanceof JSONObject) {
                assertJSONEquals((JSONObject) val, (JSONObject) obj2.get(obj1Key));
            } else {
                assertTrue(obj1.get(obj1Key).equals(obj2.get(obj1Key)));
            }
        }
    }

    private Set<String> setToString(Set set) {
        Set<String> str = Sets.newHashSet();
        for (Object obj : set) {
            str.add(obj.toString());
        }

        return str;
    }

    private Vertex createTermMention(Vertex outVertex, String sign, String conceptIri, int start, int end) {
        return createTermMention(outVertex, sign, conceptIri, start, end, null, null);
    }

    private Vertex createTermMention(Vertex outVertex, String sign, String conceptIri, int start, int end, Vertex resolvedToVertex, Edge resolvedEdge) {
        TermMentionBuilder tmb = new TermMentionBuilder()
                .outVertex(outVertex)
                .propertyKey(PROPERTY_KEY)
                .conceptIri(conceptIri)
                .start(start)
                .end(end)
                .title(sign)
                .visibilityJson("")
                .process(getClass().getSimpleName());
        if (resolvedToVertex != null || resolvedEdge != null) {
            tmb.resolvedTo(resolvedToVertex, resolvedEdge);
        }
        return tmb.save(graph, visibilityTranslator, user, authorizations);
    }

    private Vertex createTermMention(Vertex outVertex, String sign, String conceptIri, int start, int end, String process) {
        return new TermMentionBuilder()
                .outVertex(outVertex)
                .propertyKey(PROPERTY_KEY)
                .conceptIri(conceptIri)
                .start(start)
                .end(end)
                .title(sign)
                .visibilityJson("")
                .process(process)
                .save(graph, visibilityTranslator, user, authorizations);
    }

    @Test
    public void testGetHighlightedTextOverlaps() throws Exception {
        Vertex outVertex = graph.addVertex("1", visibility, authorizations);

        ArrayList<Vertex> terms = new ArrayList<>();
        terms.add(createTermMention(outVertex, "joe ferner", PERSON_IRI, 18, 28));
        terms.add(createTermMention(outVertex, "jeff kunkle", PERSON_IRI, 18, 21));
        List<OffsetItem> termAndTermMetadata = new EntityHighlighter().convertTermMentionsToOffsetItems(terms, "", authorizations);
        String highlightText = EntityHighlighter.getHighlightedText("Test highlight of Joe Ferner.", termAndTermMetadata);
        assertHighlightedTextSame("Test highlight of <span class=\"vertex\" title=\"jeff kunkle\" data-info=\"{&quot;http://visallo.org#conceptType&quot;:&quot;http://visallo.org/test/person&quot;,&quot;process&quot;:&quot;EntityHighlighterTest&quot;,&quot;start&quot;:18,&quot;end&quot;:21,&quot;id&quot;:&quot;TM_1--18-21-EntityHighlighterTest&quot;,&quot;outVertexId&quot;:&quot;1&quot;,&quot;type&quot;:&quot;http://www.w3.org/2002/07/owl#Thing&quot;,&quot;title&quot;:&quot;jeff kunkle&quot;,&quot;sandboxStatus&quot;:&quot;PRIVATE&quot;}\">Joe</span> Ferner.", highlightText);
    }

    @Test
    public void testGetHighlightedTextExactOverlapsTwoDocuments() {
        Authorizations tmAuths = graph.createAuthorizations(TermMentionRepository.VISIBILITY_STRING);

        Vertex v1 = graph.addVertex("v1", visibility, authorizations);
        Vertex v2 = graph.addVertex("v2", visibility, authorizations);
        Vertex vjf = graph.addVertex("jf", visibility, authorizations);
        Edge e1 = graph.addEdge("e1", v1, vjf, "has", visibility, authorizations);
        Edge e2 = graph.addEdge("e2", v2, vjf, "has", visibility, authorizations);

        createTermMention(v1, "joe ferner", PERSON_IRI, 18, 28, vjf, e1);
        createTermMention(v2, "joe ferner", PERSON_IRI, 18, 28, vjf, e2);
        graph.flush();

        ArrayList<Vertex> terms = Lists.newArrayList(graph.getVertex("v1", tmAuths).getVertices(Direction.BOTH, VisalloProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, tmAuths));
        List<OffsetItem> termAndTermMetadata = new EntityHighlighter().convertTermMentionsToOffsetItems(terms, "", this.authorizations);
        String highlightText = EntityHighlighter.getHighlightedText("Test highlight of Joe Ferner.", termAndTermMetadata);
        assertHighlightedTextSame("Test highlight of <span class=\"vertex resolved\" title=\"joe ferner\" data-info=\"{&quot;http://visallo.org#conceptType&quot;:&quot;http://visallo.org/test/person&quot;,&quot;process&quot;:&quot;EntityHighlighterTest&quot;,&quot;resolvedToVertexId&quot;:&quot;jf&quot;,&quot;resolvedToEdgeId&quot;:&quot;e1&quot;,&quot;start&quot;:18,&quot;termMentionFor&quot;:&quot;VERTEX&quot;,&quot;end&quot;:28,&quot;id&quot;:&quot;TM_v1--18-28-EntityHighlighterTest&quot;,&quot;outVertexId&quot;:&quot;v1&quot;,&quot;type&quot;:&quot;http://www.w3.org/2002/07/owl#Thing&quot;,&quot;title&quot;:&quot;joe ferner&quot;,&quot;sandboxStatus&quot;:&quot;PRIVATE&quot;}\">Joe Ferner</span>.", highlightText);

        terms = Lists.newArrayList(graph.getVertex("v2", tmAuths).getVertices(Direction.BOTH, VisalloProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, tmAuths));
        termAndTermMetadata = new EntityHighlighter().convertTermMentionsToOffsetItems(terms, "", this.authorizations);
        highlightText = EntityHighlighter.getHighlightedText("Test highlight of Joe Ferner.", termAndTermMetadata);
        assertHighlightedTextSame("Test highlight of <span class=\"vertex resolved\" title=\"joe ferner\" data-info=\"{&quot;http://visallo.org#conceptType&quot;:&quot;http://visallo.org/test/person&quot;,&quot;process&quot;:&quot;EntityHighlighterTest&quot;,&quot;resolvedToVertexId&quot;:&quot;jf&quot;,&quot;resolvedToEdgeId&quot;:&quot;e2&quot;,&quot;start&quot;:18,&quot;termMentionFor&quot;:&quot;VERTEX&quot;,&quot;end&quot;:28,&quot;id&quot;:&quot;TM_v2--18-28-EntityHighlighterTest&quot;,&quot;outVertexId&quot;:&quot;v2&quot;,&quot;type&quot;:&quot;http://www.w3.org/2002/07/owl#Thing&quot;,&quot;title&quot;:&quot;joe ferner&quot;,&quot;sandboxStatus&quot;:&quot;PRIVATE&quot;}\">Joe Ferner</span>.", highlightText);
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
        Vertex outVertex = graph.addVertex("1", visibility, authorizations);

        ArrayList<Vertex> terms = new ArrayList<>();
        terms.add(createTermMention(outVertex, "US", LOCATION_IRI, 48, 50));
        List<OffsetItem> termAndTermMetadata = new EntityHighlighter().convertTermMentionsToOffsetItems(terms, "", authorizations);

        String highlightText = EntityHighlighter.getHighlightedText("Ejército de Liberación Nacional® partnered with US on peace treaty", termAndTermMetadata);
        String expectedText = "Ej&eacute;rcito de Liberaci&oacute;n Nacional&reg; partnered with <span class=\"vertex\" title=\"US\" data-info=\"{&quot;process&quot;:&quot;EntityHighlighterTest&quot;,&quot;id&quot;:&quot;TM_1--48-50-EntityHighlighterTest&quot;,&quot;title&quot;:&quot;US&quot;,&quot;sandboxStatus&quot;:&quot;PRIVATE&quot;,&quot;start&quot;:48,&quot;outVertexId&quot;:&quot;1&quot;,&quot;http://visallo.org#conceptType&quot;:&quot;http://visallo.org/test/location&quot;,&quot;type&quot;:&quot;http://www.w3.org/2002/07/owl#Thing&quot;,&quot;end&quot;:50}\">US</span> on peace treaty";

        assertHighlightedTextSame(expectedText, highlightText);
    }

    private List<String> asList(String[] strings) {
        List<String> results = new ArrayList<>();
        Collections.addAll(results, strings);
        return results;
    }
}
