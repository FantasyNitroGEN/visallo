package org.visallo.web.structuredingest.mapping;

import com.google.common.collect.Maps;

import java.util.Map;

public class MappingTestHelpers {
    public static Map<String, Object> createIndexedMap(String... values){
        Map<String, Object> map = Maps.newHashMap();
        for(int i = 0; i < values.length; i++){
            map.put("" + i, values[i]);
        }
        return map;
    }
}
