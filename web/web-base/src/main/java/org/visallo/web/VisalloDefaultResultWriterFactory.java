package org.visallo.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.v5analytics.webster.resultWriters.ResultWriter;
import com.v5analytics.webster.resultWriters.ResultWriterBase;
import com.v5analytics.webster.resultWriters.ResultWriterFactory;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.security.ACLProvider;
import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.clientapi.util.ObjectMapperFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

public class VisalloDefaultResultWriterFactory implements ResultWriterFactory {
    private ACLProvider aclProvider;

    @Inject
    public VisalloDefaultResultWriterFactory(ACLProvider aclProvider) {
        this.aclProvider = aclProvider;
    }

    @Override
    public ResultWriter createResultWriter(Method handleMethod) {
        return new ResultWriterBase(handleMethod) {
            private boolean resultIsClientApiObject;
            private boolean resultIsInputStream;

            @Override
            protected String getContentType(Method handleMethod) {
                if (JSONObject.class.equals(handleMethod.getReturnType())) {
                    return "application/json";
                }
                if (ClientApiObject.class.isAssignableFrom(handleMethod.getReturnType())) {
                    resultIsClientApiObject = true;
                    return "application/json";
                }
                if (InputStream.class.isAssignableFrom(handleMethod.getReturnType())) {
                    resultIsInputStream = true;
                }
                return super.getContentType(handleMethod);
            }

            @Override
            protected void writeResult(HttpServletResponse response, Object result) throws IOException {
                if (result != null) {
                    response.setCharacterEncoding("UTF-8");
                    if (resultIsClientApiObject) {
                        try {
                            String jsonObject = ObjectMapperFactory.getInstance().writeValueAsString(aclProvider.appendACL((ClientApiObject) result));
                            response.getWriter().write(jsonObject);
                        } catch (JsonProcessingException e) {
                            throw new VisalloException("Could not write json", e);
                        }
                    } else if (resultIsInputStream) {
                        try (InputStream in = (InputStream) result) {
                            IOUtils.copy(in, response.getOutputStream());
                        } finally {
                            response.flushBuffer();
                        }
                    } else {
                        super.writeResult(response, result);
                    }
                }
            }
        };
    }
}
