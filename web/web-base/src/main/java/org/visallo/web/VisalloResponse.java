package org.visallo.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloException;
import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.clientapi.util.ObjectMapperFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class VisalloResponse {
    public static final int EXPIRES_1_HOUR = 60 * 60;
    public static final ClientApiSuccess SUCCESS = new ClientApiSuccess();
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
            configureResponse(ResponseTypes.JSON_OBJECT, response, jsonObject);
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

    public void respondWithSuccessJson() {
        JSONObject successJson = new JSONObject();
        successJson.put("success", true);
        respondWithJson(successJson);
    }

    public void respondWithJson(JSONObject jsonObject) {
        configureResponse(ResponseTypes.JSON_OBJECT, response, jsonObject);
    }

    public void respondWithHtml(final String html) {
        configureResponse(ResponseTypes.HTML, response, html);
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

    public void flushBuffer() {
        try {
            response.flushBuffer();
        } catch (IOException e) {
            throw new VisalloException("Could not flush response buffer");
        }
    }

    public void setStatus(int statusCode) {
        response.setStatus(statusCode);
    }

    public void setContentLength(int length) {
        response.setContentLength(length);
    }

    public void setCharacterEncoding(String charset) {
        response.setCharacterEncoding(charset);
    }

    public static void configureResponse(final ResponseTypes type, final HttpServletResponse response, final Object responseData) {
        Preconditions.checkNotNull(response, "The provided response was invalid");
        Preconditions.checkNotNull(responseData, "The provided data was invalid");

        try {
            switch (type) {
                case JSON_OBJECT:
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(responseData.toString());
                    break;
                case JSON_ARRAY:
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(responseData.toString());
                    break;
                case PLAINTEXT:
                    response.setContentType("text/plain");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(responseData.toString());
                    break;
                case HTML:
                    response.setContentType("text/html");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(responseData.toString());
                    break;
                default:
                    throw new VisalloException("Unsupported response type encountered");
            }

            if (response.getWriter().checkError()) {
                throw new ConnectionClosedException();
            }
        } catch (IOException e) {
            throw new VisalloException("Error occurred while writing response", e);
        }
    }

    public HttpServletResponse getHttpServletResponse() {
        return response;
    }
}
