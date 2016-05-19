package org.visallo.common.rdf;

import org.junit.Test;

public class ConceptTypeVisalloRdfTripleTest extends VisalloRdfTripleTestBase {
    @Test
    public void testToString() {
        ConceptTypeVisalloRdfTriple triple = new ConceptTypeVisalloRdfTriple("v1", "A", "concept1");
        assertEqualsVisalloRdfTriple("<v1[A]> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <concept1>", triple);
    }
}