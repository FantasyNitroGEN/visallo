package org.visallo.core.model.properties.types;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonSingleValueVisalloPropertyTest {
    @Test
    public void testEquals() {
        JsonSingleValueVisalloProperty prop = new JsonSingleValueVisalloProperty("name");
        assertTrue(prop.isEquals(new JSONObject("{a:1}"), new JSONObject("{a:1}")));
        assertFalse(prop.isEquals(new JSONObject("{a:1}"), new JSONObject("{a:1,b:2}")));
    }
}