package org.visallo.web.clientapi.model;

import java.util.HashMap;
import java.util.Map;

public class ClientApiVertexCountsByConceptType implements ClientApiObject {
    private Map<String, Long> conceptTypeCounts = new HashMap<>();

    public ClientApiVertexCountsByConceptType() {

    }

    public ClientApiVertexCountsByConceptType(Map<Object, Long> conceptTypeCounts) {
        for (Map.Entry<Object, Long> conceptTypeCount : conceptTypeCounts.entrySet()) {
            this.conceptTypeCounts.put(conceptTypeCount.getKey().toString(), conceptTypeCount.getValue());
        }
    }

    public Map<String, Long> getConceptTypeCounts() {
        return conceptTypeCounts;
    }
}
