package org.visallo.phoneNumber;

import com.google.common.base.Charsets;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerTestSetupBase;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Direction;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.VertexBuilder;
import org.vertexium.property.StreamingPropertyValue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.vertexium.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class PhoneNumberGraphPropertyWorkerTest extends GraphPropertyWorkerTestSetupBase {
    private static final String PHONE_TEXT = "This terrorist's phone number is 410-678-2230, and his best buddy's phone number is +44 (0)207 437 0478";
    private static final String PHONE_NEW_LINES = "This terrorist's phone\n number is 410-678-2230, and his best buddy's phone number\n is +44 (0)207 437 0478";
    private static final String PHONE_MISSING = "This is a sentence without any phone numbers in it.";
    private static final String MULTI_VALUE_KEY = PhoneNumberGraphPropertyWorkerTest.class.getName();

    @Override
    protected GraphPropertyWorker createGraphPropertyWorker() {
        return new PhoneNumberGraphPropertyWorker();
    }

    @Test
    public void testPhoneNumberExtraction() throws Exception {
        InputStream in = asStream(PHONE_TEXT);
        VertexBuilder vertexBuilder = graph.prepareVertex("v1", visibility);
        StreamingPropertyValue textPropertyValue = new StreamingPropertyValue(in, String.class);
        VisalloProperties.TEXT.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, textPropertyValue, visibility);
        VisalloProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, visibilityTranslator.getDefaultVisibility());
        Vertex vertex = vertexBuilder.save(authorizations);

        Property property = vertex.getProperty(VisalloProperties.TEXT.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(visibilityTranslator, vertex, property, null, null, Priority.NORMAL);
        in = asStream(PHONE_TEXT);
        worker.execute(in, workData);

        List<Vertex> termMentions = toList(vertex.getVertices(Direction.OUT,
                VisalloProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, termMentionAuthorizations));

        assertEquals("Incorrect number of phone numbers extracted", 2, termMentions.size());

        boolean foundFirst = false;
        boolean foundSecond = false;
        for (Vertex term : termMentions) {
            String title = VisalloProperties.TERM_MENTION_TITLE.getPropertyValue(term);
            if (title.equals("+14106782230")) {
                foundFirst = true;
                assertEquals(33, VisalloProperties.TERM_MENTION_START_OFFSET.getPropertyValue(term, 0));
                assertEquals(45, VisalloProperties.TERM_MENTION_END_OFFSET.getPropertyValue(term, 0));
            } else if (title.equals("+442074370478")) {
                foundSecond = true;
                assertEquals(84, VisalloProperties.TERM_MENTION_START_OFFSET.getPropertyValue(term, 0));
                assertEquals(103, VisalloProperties.TERM_MENTION_END_OFFSET.getPropertyValue(term, 0));
            }
        }
        assertTrue("+14106782230 not found", foundFirst);
        assertTrue("+442074370478 not found", foundSecond);
    }

    @Test
    public void testPhoneNumberExtractionWithNewlines() throws Exception {
        InputStream in = asStream(PHONE_NEW_LINES);
        VertexBuilder vertexBuilder = graph.prepareVertex("v1", visibility);
        StreamingPropertyValue textPropertyValue = new StreamingPropertyValue(in, String.class);
        VisalloProperties.TEXT.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, textPropertyValue, visibility);
        VisalloProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, visibilityTranslator.getDefaultVisibility());
        Vertex vertex = vertexBuilder.save(authorizations);

        Property property = vertex.getProperty(VisalloProperties.TEXT.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(visibilityTranslator, vertex, property, WORKSPACE_ID, VISIBILITY_SOURCE, Priority.NORMAL);
        in = asStream(PHONE_NEW_LINES);
        worker.execute(in, workData);

        List<Vertex> termMentions = toList(vertex.getVertices(Direction.OUT,
                VisalloProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, termMentionAuthorizations));

        assertEquals("Incorrect number of phone numbers extracted", 2, termMentions.size());

        boolean foundFirst = false;
        boolean foundSecond = false;
        for (Vertex term : termMentions) {
            String title = VisalloProperties.TERM_MENTION_TITLE.getPropertyValue(term);
            if (title.equals("+14106782230")) {
                foundFirst = true;
                assertEquals(34, VisalloProperties.TERM_MENTION_START_OFFSET.getPropertyValue(term, 0));
                assertEquals(46, VisalloProperties.TERM_MENTION_END_OFFSET.getPropertyValue(term, 0));
            } else if (title.equals("+442074370478")) {
                foundSecond = true;
                assertEquals(86, VisalloProperties.TERM_MENTION_START_OFFSET.getPropertyValue(term, 0));
                assertEquals(105, VisalloProperties.TERM_MENTION_END_OFFSET.getPropertyValue(term, 0));
            }
        }
        assertTrue("+14106782230 not found", foundFirst);
        assertTrue("+442074370478 not found", foundSecond);
    }

    @Test
    public void testNegativePhoneNumberExtraction() throws Exception {
        InputStream in = asStream(PHONE_MISSING);
        VertexBuilder vertexBuilder = graph.prepareVertex("v1", visibility);
        StreamingPropertyValue textPropertyValue = new StreamingPropertyValue(in, String.class);
        VisalloProperties.TEXT.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, textPropertyValue, visibility);
        Vertex vertex = vertexBuilder.save(authorizations);

        Property property = vertex.getProperty(VisalloProperties.TEXT.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(visibilityTranslator, vertex, property, null, null, Priority.NORMAL);
        in = asStream(PHONE_MISSING);
        worker.execute(in, workData);

        List<Vertex> termMentions = toList(vertex.getVertices(Direction.OUT, VisalloProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, authorizations));

        assertTrue("Phone number extracted when there were no phone numbers", termMentions.isEmpty());
    }

    private InputStream asStream(final String text) {
        return new ByteArrayInputStream(text.getBytes(Charsets.UTF_8));
    }
}
