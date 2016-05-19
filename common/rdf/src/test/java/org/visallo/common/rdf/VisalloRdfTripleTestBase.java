package org.visallo.common.rdf;

import static org.junit.Assert.assertEquals;

public abstract class VisalloRdfTripleTestBase {
    protected void assertEqualsVisalloRdfTriple(String expectedString, VisalloRdfTriple triple) {
        assertEqualsVisalloRdfTriple(expectedString, triple, true);
    }

    protected void assertEqualsVisalloRdfTriple(String expectedString, VisalloRdfTriple triple, boolean assertRoundTrip) {
        String tripleString = triple.toString();
        System.out.println("triple: " + tripleString);
        assertEquals(expectedString, tripleString);
        RdfTriple rdfTriple = RdfTripleParser.parseLine(tripleString);
        VisalloRdfTriple parserResult = VisalloRdfTriple.parse(rdfTriple, null, null, null);
        if (assertRoundTrip) {
            assertEquals(triple, parserResult);
        }
    }
}
