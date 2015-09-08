package org.visallo.tools;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RdfTripleParserTest {
    @Test
    public void testParseLinePropertyValue() throws Exception {
        RdfTriple result = RdfTripleParser.parseLine("<http://dbpedia.org/resource/Aristotle> <http://xmlns.com/foaf/0.1/name> \"Aristotle\"@en .");
        assertTrue(result.getFirst() instanceof RdfTriple.UriPart);
        assertEquals("http://dbpedia.org/resource/Aristotle", ((RdfTriple.UriPart) result.getFirst()).getUri());
        assertTrue(result.getSecond() instanceof RdfTriple.UriPart);
        assertEquals("http://xmlns.com/foaf/0.1/name", ((RdfTriple.UriPart) result.getSecond()).getUri());
        assertTrue(result.getThird() instanceof RdfTriple.LiteralPart);
        assertEquals("Aristotle", ((RdfTriple.LiteralPart) result.getThird()).getString());
        assertEquals("en", ((RdfTriple.LiteralPart) result.getThird()).getLanguage());
        assertEquals(null, ((RdfTriple.LiteralPart) result.getThird()).getType());
        assertTrue(result.getForth() instanceof RdfTriple.EmptyPart);
    }

    @Test
    public void testParseLinePropertyValueWithoutLanguage() throws Exception {
        RdfTriple result = RdfTripleParser.parseLine("<http://dbpedia.org/resource/Aristotle> <http://xmlns.com/foaf/0.1/name> \"Aristotle\" .");
        assertEquals("", ((RdfTriple.LiteralPart) result.getThird()).getLanguage());
    }

    @Test
    public void testParseLinePropertyValueWithoutLanguageAndFourth() throws Exception {
        RdfTriple result = RdfTripleParser.parseLine("<http://dbpedia.org/resource/Aristotle> <http://xmlns.com/foaf/0.1/name> \"Aristotle\"");
        assertEquals("", ((RdfTriple.LiteralPart) result.getThird()).getLanguage());
    }

    @Test
    public void testParseLineDateProperty() {
        RdfTriple result = RdfTripleParser.parseLine("<http://dbpedia.org/resource/Abraham_Lincoln> <http://dbpedia.org/ontology/birthDate> \"1809-02-12\"^^<http://www.w3.org/2001/XMLSchema#date> .");
        assertTrue(result.getFirst() instanceof RdfTriple.UriPart);
        assertEquals("http://dbpedia.org/resource/Abraham_Lincoln", ((RdfTriple.UriPart) result.getFirst()).getUri());
        assertTrue(result.getSecond() instanceof RdfTriple.UriPart);
        assertEquals("http://dbpedia.org/ontology/birthDate", ((RdfTriple.UriPart) result.getSecond()).getUri());
        assertTrue(result.getThird() instanceof RdfTriple.LiteralPart);
        assertEquals("1809-02-12", ((RdfTriple.LiteralPart) result.getThird()).getString());
        assertEquals("", ((RdfTriple.LiteralPart) result.getThird()).getLanguage());
        assertEquals("http://www.w3.org/2001/XMLSchema#date", ((RdfTriple.LiteralPart) result.getThird()).getType().getUri());
        assertTrue(result.getForth() instanceof RdfTriple.EmptyPart);
    }

    @Test
    public void testParseLineEdge() {
        RdfTriple result = RdfTripleParser.parseLine("<http://dbpedia.org/resource/Abraham_Lincoln> <http://dbpedia.org/ontology/birthPlace> <http://dbpedia.org/resource/Hardin_County,_Kentucky> .");
        assertTrue(result.getFirst() instanceof RdfTriple.UriPart);
        assertEquals("http://dbpedia.org/resource/Abraham_Lincoln", ((RdfTriple.UriPart) result.getFirst()).getUri());
        assertTrue(result.getSecond() instanceof RdfTriple.UriPart);
        assertEquals("http://dbpedia.org/ontology/birthPlace", ((RdfTriple.UriPart) result.getSecond()).getUri());
        assertTrue(result.getThird() instanceof RdfTriple.UriPart);
        assertEquals("http://dbpedia.org/resource/Hardin_County,_Kentucky", ((RdfTriple.UriPart) result.getThird()).getUri());
        assertTrue(result.getForth() instanceof RdfTriple.EmptyPart);
    }
}
