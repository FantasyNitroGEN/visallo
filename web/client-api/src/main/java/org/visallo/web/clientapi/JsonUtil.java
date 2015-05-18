package org.visallo.web.clientapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.visallo.web.clientapi.util.ObjectMapperFactory;

public class JsonUtil {
    public static ObjectMapper mapper = ObjectMapperFactory.getInstance();

    public static ObjectMapper getJsonMapper() {
        return mapper;
    }
}

