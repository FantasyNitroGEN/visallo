package org.visallo.javaCodeIngest;

import org.visallo.core.model.properties.types.StringVisalloProperty;

public class JavaCodeIngestOntology {
    public static final String EDGE_LABEL_JAR_CONTAINS = "http://visallo.org/java-code-ingest#jarFileContains";
    public static final String EDGE_LABEL_CLASS_FILE_CONTAINS_CLASS = "http://visallo.org/java-code-ingest#classFileContainsClass";
    public static final String EDGE_LABEL_CLASS_CONTAINS = "http://visallo.org/java-code-ingest#classContains";
    public static final String EDGE_LABEL_INVOKED = "http://visallo.org/java-code-ingest#invoked";
    public static final String EDGE_LABEL_FIELD_TYPE = "http://visallo.org/java-code-ingest#fieldType";
    public static final String EDGE_LABEL_METHOD_RETURN_TYPE = "http://visallo.org/java-code-ingest#methodReturnType";
    public static final String EDGE_LABEL_METHOD_ARGUMENT = "http://visallo.org/java-code-ingest#argument";
    public static final String EDGE_LABEL_CLASS_REFERENCES = "http://visallo.org/java-code-ingest#classReferences";
    public static final String CONCEPT_TYPE_JAR_FILE = "http://visallo.org/java-code-ingest#jarFile";
    public static final String CONCEPT_TYPE_CLASS_FILE = "http://visallo.org/java-code-ingest#classFile";
    public static final String CONCEPT_TYPE_CLASS = "http://visallo.org/java-code-ingest#class";
    public static final String CONCEPT_TYPE_INTERFACE = "http://visallo.org/java-code-ingest#interface";
    public static final String CONCEPT_TYPE_METHOD = "http://visallo.org/java-code-ingest#method";
    public static final String CONCEPT_TYPE_FIELD = "http://visallo.org/java-code-ingest#field";

    public static final StringVisalloProperty CLASS_NAME = new StringVisalloProperty("http://visallo.org/java-code-ingest#className");
    public static final StringVisalloProperty ARGUMENT_NAME = new StringVisalloProperty("http://visallo.org/java-code-ingest#argumentName");
    public static final StringVisalloProperty FIELD_NAME = new StringVisalloProperty("http://visallo.org/java-code-ingest#fieldName");
    public static final StringVisalloProperty JAR_ENTRY_NAME = new StringVisalloProperty("http://visallo.org/java-code-ingest#jarEntryName");
    public static final StringVisalloProperty METHOD_NAME = new StringVisalloProperty("http://visallo.org/java-code-ingest#methodName");
    public static final StringVisalloProperty METHOD_SIGNATURE = new StringVisalloProperty("http://visallo.org/java-code-ingest#methodSignature");
}
