package org.visallo.common.rdf;

import org.junit.Test;

public class AddEdgeVisalloRdfTripleTest extends VisalloRdfTripleTestBase {
    @Test
    public void testToString() {
        AddEdgeVisalloRdfTriple triple = new AddEdgeVisalloRdfTriple("e1", "vout", "vin", "http://visallo.org#edgeLabel", "A");
        assertEqualsVisalloRdfTriple("<vout> <http://visallo.org#edgeLabel:e1[A]> <vin>", triple);
    }

    @Test
    public void testToStringWhenIdHasAColor() {
        AddEdgeVisalloRdfTriple triple = new AddEdgeVisalloRdfTriple("e:1", "vout", "vin", "http://visallo.org#edgeLabel", "A");
        assertEqualsVisalloRdfTriple("<vout> <http://visallo.org#edgeLabel:e\\:1[A]> <vin>", triple);
    }
}