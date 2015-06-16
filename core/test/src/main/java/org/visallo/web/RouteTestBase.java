package org.visallo.web;

import com.amazonaws.util.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.v5analytics.webster.HandlerChain;
import org.json.JSONArray;
import org.mockito.Mock;
import org.vertexium.Graph;
import org.vertexium.inmemory.InMemoryGraph;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.ConfigurationLoader;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.ProxyUser;
import org.visallo.core.user.User;
import org.visallo.vertexium.model.user.InMemoryUser;
import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.util.ObjectMapperFactory;
import org.visallo.web.routes.workspace.WorkspaceHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
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
    protected OntologyRepository ontologyRepository;

    @Mock
    protected WorkspaceRepository workspaceRepository;

    @Mock
    protected TermMentionRepository termMentionRepository;

    @Mock
    protected WorkspaceHelper workspaceHelper;

    protected Configuration configuration;

    protected Graph graph;

    @Mock
    protected HttpSession httpSession;

    protected SessionUser sessionUser;

    protected ProxyUser user;

    protected User nonProxiedUser;

    private ByteArrayOutputStream responseByteArrayOutputStream;

    private ObjectMapper objectMapper;

    protected void setUp() throws IOException {
        Map config = new HashMap();
        ConfigurationLoader hashMapConfigurationLoader = new HashMapConfigurationLoader(config);
        configuration = new Configuration(hashMapConfigurationLoader, new HashMap<>());

        objectMapper = ObjectMapperFactory.getInstance();

        graph = InMemoryGraph.create();

        Set<Privilege> privileges = new HashSet<>();
        String[] authorizations = new String[0];
        String currentWorkspaceId = null;
        nonProxiedUser = new InMemoryUser("jdoe", "Jane Doe", "jane.doe@email.com", privileges, authorizations, currentWorkspaceId);
        when(userRepository.findById(eq(USER_ID))).thenReturn(nonProxiedUser);

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

    protected void handleAssertSuccess(BaseRequestHandler handler) throws Exception {
        handle(handler);
        org.json.JSONObject successJson = new org.json.JSONObject();
        successJson.put("success", true);
        assertEquals(successJson.toString(), getResponseAsString());
    }

    protected <T extends ClientApiObject> T handle(BaseRequestHandler handler, Class<T> responseType) throws Exception {
        handler.handle(request, response, chain);
        return getResponseAsClientApiObject(responseType);
    }

    protected void setArrayParameter(String parameterName, String[] values) {
        when(request.getParameterValues(eq(parameterName))).thenReturn(values);
    }

    protected void setParameter(String parameterName, JSONObject json) {
        setParameter(parameterName, json.toString());
    }

    protected void setParameter(String parameterName, JSONArray json) {
        setParameter(parameterName, json.toString());
    }

    protected void setParameter(String parameterName, String value) {
        when(request.getParameter(eq(parameterName))).thenReturn(value);
    }
}
