package org.visallo.core.util;

import org.vertexium.Element;
import org.vertexium.Property;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.web.clientapi.model.SandboxStatus;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.util.Collection;
import java.util.List;

public class SandboxStatusUtil {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(SandboxStatusUtil.class);

    public static SandboxStatus getSandboxStatus(Element element, String workspaceId) {
        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(element);
        return SandboxStatus.getFromVisibilityJsonString(visibilityJson, workspaceId);
    }

    public static SandboxStatus[] getPropertySandboxStatuses(List<Property> properties, String workspaceId) {
        SandboxStatus[] sandboxStatuses = new SandboxStatus[properties.size()];
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            Collection<VisibilityJson> visibilityJsons = VisalloProperties.VISIBILITY_JSON_METADATA.getMetadataValues(property.getMetadata());
            if (visibilityJsons.size() > 1) {
                LOGGER.error("Multiple %s found on property %s. Choosing the best match.", VisalloProperties.VISIBILITY_JSON_METADATA.getMetadataKey(), property);
            }
            sandboxStatuses[i] = getMostExclusiveSandboxStatus(visibilityJsons, workspaceId);
        }

        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            if (sandboxStatuses[i] != SandboxStatus.PRIVATE) {
                continue;
            }
            for (int j = 0; j < properties.size(); j++) {
                Property p = properties.get(j);
                if (i == j) {
                    continue;
                }

                if (sandboxStatuses[j] == SandboxStatus.PUBLIC &&
                        sandboxStatuses[i] == SandboxStatus.PRIVATE &&
                        property.getKey().equals(p.getKey()) &&
                        property.getName().equals(p.getName())) {
                    sandboxStatuses[i] = SandboxStatus.PUBLIC_CHANGED;
                }
            }
        }

        return sandboxStatuses;
    }

    private static SandboxStatus getMostExclusiveSandboxStatus(Collection<VisibilityJson> visibilityJsons, String workspaceId) {
        for (VisibilityJson visibilityJson : visibilityJsons) {
            SandboxStatus status = SandboxStatus.getFromVisibilityJsonString(visibilityJson, workspaceId);
            switch (status) {
                case PUBLIC:
                    break;
                case PUBLIC_CHANGED:
                case PRIVATE:
                    return status;
            }
        }
        return SandboxStatus.PUBLIC;
    }
}
