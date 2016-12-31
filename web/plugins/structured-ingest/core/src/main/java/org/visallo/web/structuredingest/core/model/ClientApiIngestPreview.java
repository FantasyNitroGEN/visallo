package org.visallo.web.structuredingest.core.model;

import com.google.common.collect.Maps;
import org.visallo.web.clientapi.model.ClientApiObject;

import java.util.Map;

public class ClientApiIngestPreview implements ClientApiObject {
    public Long processedRows = 0L;
    public boolean didTruncate = false;
    public Preview vertices = new Preview();
    public Preview edges = new Preview();

    public void incrementVertices(String type, boolean isNew) {
        vertices.incrementType(type, isNew);
    }

    public void incrementEdges(String label, boolean isNew) {
        edges.incrementType(label, isNew);
    }

    static class Preview {
        public Map<String, Numbers> numbers = Maps.newHashMap();

        public void incrementType(String type, boolean isNew) {

            if (!numbers.containsKey(type)) {
                numbers.put(type, new Numbers());
            }

            Numbers n = numbers.get(type);
            if (isNew) {
                n.created++;
            } else {
                n.referenced++;
            }
        }
    }

    static class Numbers {
        public Long created = 0L;
        public Long referenced = 0L;
    }
}
