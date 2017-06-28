package org.visallo.phoneNumber;

import com.google.common.base.Charsets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.*;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerTestBase;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.vertexium.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class PhoneNumberGraphPropertyWorkerTest extends GraphPropertyWorkerTestBase {
    private static final String PHONE_TEXT = "This terrorist's phone number is 410-678-2230, and his best buddy's phone number is +44 (0)207 437 0478";
    private static final String PHONE_NEW_LINES = "This terrorist's phone\n number is 410-678-2230, and his best buddy's phone number\n is +44 (0)207 437 0478";
    private static final String PHONE_MISSING = "This is a sentence without any phone numbers in it.";
    private static final String MULTI_VALUE_KEY = PhoneNumberGraphPropertyWorkerTest.class.getName();

    private PhoneNumberGraphPropertyWorker gpw;
    private VisibilityJson visibilityJson;
    private Visibility visibility;
    private Authorizations authorizations;
    private Authorizations termMentionAuthorizations;

    @Before
    public void setup() throws Exception {
        when(ontologyRepository.getRequiredConceptIRIByIntent("phoneNumber")).thenReturn("http://visallo.org/test#phoneNumber");

        gpw = new PhoneNumberGraphPropertyWorker();
        prepare(gpw);

        visibilityJson = new VisibilityJson("PhoneNumberGraphPropertyWorkerTest");
        visibility = getVisibilityTranslator().toVisibility(visibilityJson).getVisibility();
        authorizations = getGraph().createAuthorizations("PhoneNumberGraphPropertyWorkerTest");
        termMentionAuthorizations = getGraph().createAuthorizations(authorizations, TermMentionRepository.VISIBILITY_STRING);
    }

    @Test
    public void testPhoneNumberExtraction() throws Exception {
        doPhoneNumberExtraction(PHONE_TEXT, 33L, 84L);
    }

    @Test
    public void testPhoneNumberExtractionWithNewlines() throws Exception {
        doPhoneNumberExtraction(PHONE_NEW_LINES, 34L, 86L);
    }

    @Test
    public void testNegativePhoneNumberExtraction() throws Exception {
        doPhoneNumberExtraction(PHONE_MISSING, null, null);
    }

    private void doPhoneNumberExtraction(String text, Long expectedFirstOffset, Long expectedSecondOffset) {
        VertexBuilder vertexBuilder = getGraph().prepareVertex("v1", visibility);

        Metadata textMetadata = new Metadata();
        VisalloProperties.MIME_TYPE_METADATA.setMetadata(textMetadata, "text/plain", getVisibilityTranslator().getDefaultVisibility());
        StreamingPropertyValue textPropertyValue = new StreamingPropertyValue(asStream(text), String.class);
        VisalloProperties.TEXT.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, textPropertyValue, textMetadata, visibility);

        VisalloProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, getVisibilityTranslator().getDefaultVisibility());

        Vertex vertex = vertexBuilder.save(authorizations);
        Property property = vertex.getProperty(VisalloProperties.TEXT.getPropertyName());
        run(gpw, getWorkerPrepareData(), vertex, property, asStream(text));

        List<Vertex> termMentions = toList(vertex.getVertices(Direction.OUT,
                VisalloProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, termMentionAuthorizations));

        if (expectedFirstOffset != null && expectedSecondOffset != null) {
            assertEquals("Incorrect number of phone numbers extracted", 2, termMentions.size());

            boolean foundFirst = false;
            boolean foundSecond = false;
            for (Vertex term : termMentions) {
                String title = VisalloProperties.TERM_MENTION_TITLE.getPropertyValue(term);
                if ("+14106782230".equals(title)) {
                    foundFirst = true;
                    assertEquals(expectedFirstOffset.longValue(), VisalloProperties.TERM_MENTION_START_OFFSET.getPropertyValue(term, 0));
                    assertEquals(expectedFirstOffset + 12, VisalloProperties.TERM_MENTION_END_OFFSET.getPropertyValue(term, 0));
                } else if ("+442074370478".equals(title)) {
                    foundSecond = true;
                    assertEquals(expectedSecondOffset.longValue(), VisalloProperties.TERM_MENTION_START_OFFSET.getPropertyValue(term, 0));
                    assertEquals(expectedSecondOffset + 19, VisalloProperties.TERM_MENTION_END_OFFSET.getPropertyValue(term, 0));
                }
            }
            assertTrue("+14106782230 not found", foundFirst);
            assertTrue("+442074370478 not found", foundSecond);
        } else {
            assertTrue("Phone number extracted when there were no phone numbers", termMentions.isEmpty());
        }
    }

    private InputStream asStream(final String text) {
        return new ByteArrayInputStream(text.getBytes(Charsets.UTF_8));
    }
}
