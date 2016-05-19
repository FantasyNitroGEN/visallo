package org.visallo.common.rdf;

import org.junit.Test;
import org.vertexium.ElementType;

public class SetMetadataVisalloRdfTripleTest extends VisalloRdfTripleTestBase {
    @Test
    public void testToStringVertex() {
        SetMetadataVisalloRdfTriple triple = new SetMetadataVisalloRdfTriple(
                ElementType.VERTEX,
                "v1", "A",
                "pkey", "http://visallo.org#pname", "B",
                "meta1", "C",
                "value1"
        );
        assertEqualsVisalloRdfTriple("<v1[A]> <http://visallo.org#pname:pkey[B]@meta1[C]> \"value1\"", triple);
    }

    @Test
    public void testToStringEdge() {
        SetMetadataVisalloRdfTriple triple = new SetMetadataVisalloRdfTriple(
                ElementType.EDGE,
                "e1", "A",
                "pkey", "http://visallo.org#pname", "B",
                "meta1", "C",
                "value1"
        );
        assertEqualsVisalloRdfTriple("<EDGE:e1[A]> <http://visallo.org#pname:pkey[B]@meta1[C]> \"value1\"", triple);
    }
}