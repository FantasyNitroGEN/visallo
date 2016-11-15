package org.visallo.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.v5analytics.webster.resultWriters.ResultWriter;
import com.v5analytics.webster.resultWriters.ResultWriterBase;
import com.v5analytics.webster.resultWriters.ResultWriterFactory;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.security.ACLProvider;
import org.visallo.core.trace.Trace;
import org.visallo.core.trace.TraceSpan;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.clientapi.util.ObjectMapperFactory;
import org.visallo.web.parameterProviders.VisalloBaseParameterProvider;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

public class VisalloDefaultResultWriterFactory implements ResultWriterFactory {
    public static final String WEB_RESPONSE_HEADER_X_FRAME_OPTIONS = "web.response.header.X-Frame-Options";
    public static final String WEB_RESPONSE_HEADER_X_FRAME_OPTIONS_DEFAULT = "DENY";
    private final String responseHeaderXFrameOptions;
    private ACLProvider aclProvider;
    private UserRepository userRepository;

    @Inject
    public VisalloDefaultResultWriterFactory(
            ACLProvider aclProvider,
            UserRepository userRepository,
            Configuration configuration
    ) {
        this.aclProvider = aclProvider;
        this.userRepository = userRepository;
        this.responseHeaderXFrameOptions = configuration.get(WEB_RESPONSE_HEADER_X_FRAME_OPTIONS, WEB_RESPONSE_HEADER_X_FRAME_OPTIONS_DEFAULT);
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
            protected void writeResult(HttpServletRequest request, HttpServletResponse response, Object result)
                    throws IOException {
                if (result != null) {
                    if (!response.containsHeader("X-Frame-Options")) {
                        response.addHeader("X-Frame-Options", responseHeaderXFrameOptions);
                    }
                    if (!response.containsHeader("X-Content-Type-Options")) {
                        response.addHeader("X-Content-Type-Options", "nosniff");
                    }
                    response.setCharacterEncoding("UTF-8");
                    if (resultIsClientApiObject || result instanceof JSONObject) {
                        response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                        response.addHeader("Pragma", "no-cache");
                        response.addHeader("Expires", "0");
                    }
                    if (resultIsClientApiObject) {
                        ClientApiObject clientApiObject = (ClientApiObject) result;
                        User user = VisalloBaseParameterProvider.getUser(request, userRepository);
                        try (TraceSpan ignored = Trace.start("aclProvider.appendACL")) {
                            clientApiObject = aclProvider.appendACL(clientApiObject, user);
                        }
                        String jsonObject;
                        try {
                            jsonObject = ObjectMapperFactory.getInstance().writeValueAsString(clientApiObject);
                        } catch (JsonProcessingException e) {
                            throw new VisalloException("Could not convert clientApiObject to string", e);
                        }
                        response.getWriter().write(jsonObject);
                    } else if (resultIsInputStream) {
                        try (InputStream in = (InputStream) result) {
                            IOUtils.copy(in, response.getOutputStream());
                        } finally {
                            response.flushBuffer();
                        }
                    } else {
                        super.writeResult(request, response, result);
                    }
                }
            }
        };
    }
}
