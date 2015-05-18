package org.visallo.core.model.ontology;

public enum LabelName {
    HAS_PROPERTY("http://visallo.org/ontology#hasProperty"),
    HAS_EDGE("http://visallo.org/ontology#hasEdge"),
    IS_A("http://visallo.org/ontology#isA"),
    INVERSE_OF("http://visallo.org/ontology#inverseOf");

    private final String text;

    LabelName(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
