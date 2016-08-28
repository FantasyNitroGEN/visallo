package org.visallo.core.cmdline;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLOntology;
import uk.ac.manchester.cs.owl.owlapi.OWLDatatypeImpl;

import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OwlToJavaTest {
    @Mock
    private OWLOntology o;

    @Mock
    private OWLDataProperty dataProperty;

    private OwlToJava owlToJava;
    private OWLDatatype dataPropertyRange;
    private String dataPropertyDisplayType;

    @Before
    public void before() {
        owlToJava = new OwlToJava() {
            @Override
            protected OWLDatatype getDataPropertyRange(OWLOntology o, OWLDataProperty dataProperty) {
                return dataPropertyRange;
            }

            @Override
            protected String getDataPropertyDisplayType(OWLOntology o, OWLDataProperty dataProperty) {
                return dataPropertyDisplayType;
            }
        };
        dataPropertyDisplayType = null;
        dataPropertyRange = null;
    }

    @Test
    public void exportDataProperty_string() throws Exception {
        String typeIri = "http://www.w3.org/2001/XMLSchema#string";
        String expectedType = "StringVisalloProperty";
        testExportDataProperty(typeIri, expectedType);
    }

    @Test
    public void exportDataProperty_string_longtext() throws Exception {
        String typeIri = "http://www.w3.org/2001/XMLSchema#string";
        String expectedType = "StreamingVisalloProperty";
        dataPropertyDisplayType = "longtext";
        testExportDataProperty(typeIri, expectedType);
    }

    @Test
    public void exportDataProperty_directoryEntry() throws Exception {
        String typeIri = "http://visallo.org#directory/entity";
        String expectedType = "DirectoryEntityVisalloProperty";
        testExportDataProperty(typeIri, expectedType);
    }

    @Test
    public void exportDataProperty_hexBinary() throws Exception {
        String typeIri = "http://www.w3.org/2001/XMLSchema#hexBinary";
        String expectedType = "StreamingVisalloProperty";
        testExportDataProperty(typeIri, expectedType);
    }

    private void testExportDataProperty(String typeIri, String expectedType) {
        SortedMap<String, String> sortedValues = new TreeMap<>();
        SortedMap<String, String> sortedIntents = new TreeMap<>();
        IRI documentIri = IRI.create("http://visallo.org/test");
        when(dataProperty.getIRI()).thenReturn(IRI.create("http://visallo.org/test#prop"));

        dataPropertyRange = new OWLDatatypeImpl(IRI.create(typeIri));
        owlToJava.exportDataProperty(sortedValues, sortedIntents, documentIri, o, dataProperty);
        assertEquals(1, sortedValues.size());
        assertEquals("public static final " + expectedType + " PROP = new " + expectedType + "(\"http://visallo.org/test#prop\");", sortedValues.get("PROP").trim());
    }
}