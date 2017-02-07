package org.visallo.core.model.properties.types;

import org.junit.Before;
import org.junit.Test;
import org.vertexium.Authorizations;
import org.vertexium.Element;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.visallo.core.model.graph.GraphUpdateContext;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloInMemoryTestBase;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class DateVisalloPropertyTest extends VisalloInMemoryTestBase {
    private User user;
    private Authorizations authorizations;

    @Before
    public void before() {
        super.before();
        user = getUserRepository().getSystemUser();
        authorizations = getAuthorizationRepository().getGraphAuthorizations(user);
    }

    @Test
    public void testUpdatePropertyIfValueIsNewer_newer() {
        Date oldValue = Date.from(ZonedDateTime.of(2017, 2, 6, 9, 30, 0, 0, ZoneId.of("UTC")).toInstant());
        Date newValue = Date.from(ZonedDateTime.of(2017, 2, 7, 9, 30, 0, 0, ZoneId.of("UTC")).toInstant());
        Date expectedValue = Date.from(ZonedDateTime.of(2017, 2, 7, 9, 30, 0, 0, ZoneId.of("UTC")).toInstant());
        testUpdatePropertyIfValueIsNewer(oldValue, newValue, expectedValue);
    }

    @Test
    public void testUpdatePropertyIfValueIsNewer_older() {
        Date oldValue = Date.from(ZonedDateTime.of(2017, 2, 6, 9, 30, 0, 0, ZoneId.of("UTC")).toInstant());
        Date newValue = Date.from(ZonedDateTime.of(2017, 2, 5, 9, 30, 0, 0, ZoneId.of("UTC")).toInstant());
        Date expectedValue = Date.from(ZonedDateTime.of(2017, 2, 6, 9, 30, 0, 0, ZoneId.of("UTC")).toInstant());
        testUpdatePropertyIfValueIsNewer(oldValue, newValue, expectedValue);
    }

    @Test
    public void testUpdatePropertyIfValueIsNewer_newValue() {
        Date oldValue = null;
        Date newValue = Date.from(ZonedDateTime.of(2017, 2, 5, 9, 30, 0, 0, ZoneId.of("UTC")).toInstant());
        Date expectedValue = Date.from(ZonedDateTime.of(2017, 2, 5, 9, 30, 0, 0, ZoneId.of("UTC")).toInstant());
        testUpdatePropertyIfValueIsNewer(oldValue, newValue, expectedValue);
    }

    private void testUpdatePropertyIfValueIsNewer(Date oldValue, Date newValue, Date expectedValue) {
        DateVisalloProperty prop = new DateVisalloProperty("name");

        Vertex v = getGraph().addVertex("v1", new Visibility(""), authorizations);
        if (oldValue != null) {
            prop.addPropertyValue(v, "key", oldValue, new Visibility(""), authorizations);
        }

        v = getGraph().getVertex("v1", authorizations);
        try (GraphUpdateContext ctx = getGraphRepository().beginGraphUpdate(Priority.NORMAL, user, authorizations)) {
            PropertyMetadata metadata = new PropertyMetadata(user, new VisibilityJson(""), new Visibility(""));
            ctx.update(v, (GraphUpdateContext.Update<Element>) elemCtx ->
                    prop.updatePropertyIfValueIsNewer(elemCtx, "key", newValue, metadata)
            );
        }

        v = getGraph().getVertex("v1", authorizations);
        Date value = prop.getPropertyValue(v, "key");
        assertEquals(expectedValue, value);
    }
}