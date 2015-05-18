package org.visallo.translate;

import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;

public interface Translator {
    String translate(String text, String language, GraphPropertyWorkData data);
}
