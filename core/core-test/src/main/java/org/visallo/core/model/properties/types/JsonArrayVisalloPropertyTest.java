package org.visallo.core.model.properties.types;

import org.json.JSONArray;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonArrayVisalloPropertyTest {
    @Test
    public void testEquals() {
        JsonArrayVisalloProperty prop = new JsonArrayVisalloProperty("name");
        assertTrue(prop.isEquals(new JSONArray("[1,2]"), new JSONArray("[1,2]")));
        assertFalse(prop.isEquals(new JSONArray("[1,2]"), new JSONArray("[1,2,3]")));
    }
}