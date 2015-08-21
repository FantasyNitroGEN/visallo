package org.visallo.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.visallo.core.exception.VisalloException;
import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.clientapi.model.ClientApiVertexDetails;
import org.visallo.web.clientapi.util.ObjectMapperFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class VisalloResponse {
    private final HttpServletResponse response;

    public VisalloResponse(HttpServletResponse response) {
        this.response = response;
    }

    public void respondWithClientApiObject(ClientApiObject obj) throws IOException {
        if (obj == null) {
            respondWithNotFound();
            return;
        }
        try {
            String jsonObject = ObjectMapperFactory.getInstance().writeValueAsString(obj);
            BaseRequestHandler.configureResponse(ResponseTypes.JSON_OBJECT, response, jsonObject);
        } catch (JsonProcessingException e) {
            throw new VisalloException("Could not write json", e);
        }
    }

    public void respondWithNotFound() throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
