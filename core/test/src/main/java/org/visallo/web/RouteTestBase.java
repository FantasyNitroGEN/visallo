package org.visallo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.v5analytics.webster.HandlerChain;
import org.mockito.Mock;
import org.vertexium.Graph;
import org.vertexium.inmemory.InMemoryGraph;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.ConfigurationLoader;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.ProxyUser;
import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.clientapi.util.ObjectMapperFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public abstract class RouteTestBase {
    public static final String WORKSPACE_ID = "WORKSPACE_12345";
    public static final String USER_ID = "USER_123";

    @Mock
    protected UserRepository userRepository;

    @Mock
    protected HttpServletRequest request;

    @Mock
    protected HttpServletResponse response;

    @Mock
    protected HandlerChain chain;

    @Mock
    protected WorkspaceRepository workspaceRepository;

    protected Configuration configuration;

    protected Graph graph;

    @Mock
    protected HttpSession httpSession;

    protected SessionUser sessionUser;

    protected ProxyUser user;

    private ByteArrayOutputStream responseByteArrayOutputStream;

    private ObjectMapper objectMapper;

    protected void setUp() throws IOException {
        Map config = new HashMap();
        ConfigurationLoader hashMapConfigurationLoader = new HashMapConfigurationLoader(config);
        configuration = new Configuration(hashMapConfigurationLoader, new HashMap<>());

        objectMapper = ObjectMapperFactory.getInstance();

        graph = InMemoryGraph.create();

        sessionUser = new SessionUser(USER_ID);
        user = new ProxyUser(USER_ID, userRepository);

        when(request.getSession()).thenReturn(httpSession);
        when(httpSession.getAttribute(eq(CurrentUser.SESSIONUSER_ATTRIBUTE_NAME))).thenReturn(sessionUser);

        when(request.getAttribute(eq("workspaceId"))).thenReturn(WORKSPACE_ID);

        when(workspaceRepository.hasReadPermissions(eq(WORKSPACE_ID), eq(user))).thenReturn(true);

        responseByteArrayOutputStream = new ByteArrayOutputStream();
        when(response.getWriter()).thenReturn(new PrintWriter(responseByteArrayOutputStream));
    }

    protected byte[] getResponse() {
        return responseByteArrayOutputStream.toByteArray();
    }

    protected String getResponseAsString() {
        return new String(getResponse());
    }

    protected <T extends ClientApiObject> T getResponseAsClientApiObject(Class<T> type) throws IOException {
        return objectMapper.readValue(getResponse(), type);
    }

    protected void handle(BaseRequestHandler handler) throws Exception {
        handler.handle(request, response, chain);
    }
}
