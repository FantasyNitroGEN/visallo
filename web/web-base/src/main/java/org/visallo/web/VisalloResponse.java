package org.visallo.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloException;
import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.clientapi.util.ObjectMapperFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class VisalloResponse {
    public static final int EXPIRES_1_HOUR = 60 * 60;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public VisalloResponse(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
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

    public void respondWithNotFound(String message) throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, message);
    }

    public void respondWithBadRequest(final String parameterName, final String errorMessage, final String invalidValue) throws IOException {
        List<String> values = null;
        if (invalidValue != null) {
            values = new ArrayList<>();
            values.add(invalidValue);
        }
        respondWithBadRequest(parameterName, errorMessage, values);
    }

    public void respondWithBadRequest(final String parameterName, final String errorMessage, final List<String> invalidValues) throws IOException {
        JSONObject error = new JSONObject();
        error.put(parameterName, errorMessage);
        if (invalidValues != null) {
            JSONArray values = new JSONArray();
            for (String v : invalidValues) {
                values.put(v);
            }
            error.put("invalidValues", values);
        }
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        respondWithJson(error);
    }

    public void respondWithBadRequest(final String parameterName, final String errorMessage) throws IOException {
        respondWithBadRequest(parameterName, errorMessage, new ArrayList<String>());
    }

    public HttpServletResponse getHttpServletResponse() {
        return response;
    }

    public void respondWithSuccessJson() {
        JSONObject result = new JSONObject();
        result.put("success", true);
        respondWithJson(result);
    }

    public void respondWithJson(JSONObject jsonObject) {
        BaseRequestHandler.configureResponse(ResponseTypes.JSON_OBJECT, response, jsonObject);
    }

    public void respondWithPlaintext(final String plaintext) {
        BaseRequestHandler.configureResponse(ResponseTypes.PLAINTEXT, response, plaintext);
    }

    public void respondWithHtml(final String html) {
        BaseRequestHandler.configureResponse(ResponseTypes.HTML, response, html);
    }

    public String generateETag(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] md5 = digest.digest(data);
            return Hex.encodeHexString(md5);
        } catch (NoSuchAlgorithmException e) {
            throw new VisalloException("Could not find MD5", e);
        }
    }

    public void addETagHeader(String eTag) {
        response.setHeader("ETag", "\"" + eTag + "\"");
    }

    public boolean testEtagHeaders(String eTag) throws IOException {
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null) {
            if (ifNoneMatch.startsWith("\"") && ifNoneMatch.length() > 2) {
                ifNoneMatch = ifNoneMatch.substring(1, ifNoneMatch.length() - 1);
            }
            if (eTag.equalsIgnoreCase(ifNoneMatch)) {
                addETagHeader(eTag);
                respondWithNotModified();
                return true;
            }
        }

        return false;
    }

    public void respondWithNotModified() throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
    }

    public void write(byte[] bytes) throws IOException {
        ServletOutputStream out = response.getOutputStream();
        out.write(bytes);
        out.close();
    }

    public void write(InputStream in) throws IOException {
        ServletOutputStream out = response.getOutputStream();
        IOUtils.copy(in, out);
        out.close();
    }

    public void setContentType(String contentType) {
        response.setContentType(contentType);
    }

    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    public void addHeader(String name, String value) {
        response.addHeader(name, value);
    }

    public void setMaxAge(int numberOfSeconds) {
        response.setHeader("Cache-Control", "max-age=" + numberOfSeconds);
    }

    public OutputStream getOutputStream() {
        try {
            return response.getOutputStream();
        } catch (IOException e) {
            throw new VisalloException("Could not get response output stream", e);
        }
    }

    public void setContentLength(int length) {
        response.setContentLength(length);
    }
}
