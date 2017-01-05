package org.visallo.web.clientapi.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.json.JSONObject;

import java.io.IOException;

public class ObjectMapperFactory {
    private static ObjectMapper mapper;

    public static ObjectMapper getInstance() {
        if (mapper == null) {
            mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.registerModule(createVisalloObjectMapperModule());
        }
        return mapper;
    }

    private static Module createVisalloObjectMapperModule() {
        SimpleModule visalloModule = new SimpleModule();
        visalloModule.addSerializer(JSONObject.NULL.getClass(), new JSONObjectNullSerializer());
        return visalloModule;
    }

    private static class JSONObjectNullSerializer extends JsonSerializer<Object> {
        @Override
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeNull();
        }
    }
}
