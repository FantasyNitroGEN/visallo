package org.visallo.core.config;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class HashMapConfigurationLoaderTest {
    @Test
    public void testLoadFromString() {
        Configuration config = new HashMapConfigurationLoader("prop1=test1\nprop2=${prop1}").createConfiguration();
        assertEquals("test1", config.get("prop1", "not found"));
        assertEquals("test1", config.get("prop2", "not found"));
    }

    @Test
    public void testLoadFromMap() {
        Map map = new HashMap<>();
        map.put("prop1", "test1");
        map.put("prop2", "${prop1}");
        Configuration config = new HashMapConfigurationLoader(map).createConfiguration();
        assertEquals("test1", config.get("prop1", "not found"));
        assertEquals("test1", config.get("prop2", "not found"));
    }
}