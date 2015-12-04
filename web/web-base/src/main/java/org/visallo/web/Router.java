package org.visallo.web;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.v5analytics.webster.Handler;
import com.v5analytics.webster.handlers.StaticResourceHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.geocoding.DefaultGeocoderRepository;
import org.visallo.core.geocoding.GeocoderRepository;
import org.visallo.core.trace.Trace;
import org.visallo.core.trace.TraceSpan;
import org.visallo.core.util.ServiceLoaderUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.privilegeFilters.*;
import org.visallo.web.routes.Index;
import org.visallo.web.routes.admin.AdminList;
import org.visallo.web.routes.admin.AdminUploadOntology;
import org.visallo.web.routes.admin.PluginList;
import org.visallo.web.routes.config.Configuration;
import org.visallo.web.routes.edge.*;
import org.visallo.web.routes.element.ElementGetAcl;
import org.visallo.web.routes.element.ElementSearch;
import org.visallo.web.routes.longRunningProcess.LongRunningProcessById;
import org.visallo.web.routes.longRunningProcess.LongRunningProcessCancel;
import org.visallo.web.routes.longRunningProcess.LongRunningProcessDelete;
import org.visallo.web.routes.map.GetGeocoder;
import org.visallo.web.routes.map.MapzenTileProxy;
import org.visallo.web.routes.notification.Notifications;
import org.visallo.web.routes.notification.SystemNotificationDelete;
import org.visallo.web.routes.notification.SystemNotificationSave;
import org.visallo.web.routes.notification.UserNotificationMarkRead;
import org.visallo.web.routes.ontology.Ontology;
import org.visallo.web.routes.ping.Ping;
import org.visallo.web.routes.ping.PingStats;
import org.visallo.web.routes.resource.MapMarkerImage;
import org.visallo.web.routes.resource.ResourceExternalGet;
import org.visallo.web.routes.resource.ResourceGet;
import org.visallo.web.routes.search.SearchDelete;
import org.visallo.web.routes.search.SearchList;
import org.visallo.web.routes.search.SearchRun;
import org.visallo.web.routes.search.SearchSave;
import org.visallo.web.routes.user.*;
import org.visallo.web.routes.vertex.*;
import org.visallo.web.routes.workspace.*;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.vertexium.util.IterableUtils.toList;

public class Router extends HttpServlet {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(Router.class);

    /**
     * Copied from org.eclipse.jetty.server.Request.__MULTIPART_CONFIG_ELEMENT.
     * TODO: Examine why this is necessary and how it can be abstracted to any servlet container.
     */
    private static final String JETTY_MULTIPART_CONFIG_ELEMENT8 = "org.eclipse.multipartConfig";
    private static final String JETTY_MULTIPART_CONFIG_ELEMENT9 = "org.eclipse.jetty.multipartConfig";
    private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
    private static final String GRAPH_TRACE_ENABLE = "graphTraceEnable";
    private WebApp app;
    private org.visallo.core.config.Configuration configuration;
    private GeocoderRepository geocoderRepository;

    @SuppressWarnings("unchecked")
    public Router(ServletContext servletContext) {
        try {
            final Injector injector = (Injector) servletContext.getAttribute(Injector.class.getName());
            injector.injectMembers(this);

            app = new WebApp(servletContext, injector);

            AuthenticationHandler authenticatorInstance = new AuthenticationHandler();
            Class<? extends Handler> authenticator = AuthenticationHandler.class;

            Class<? extends Handler> csrfProtector = VisalloCsrfHandler.class;

            app.get("/", UserAgentFilter.class, csrfProtector, Index.class);
            app.get("/configuration", csrfProtector, Configuration.class);
            app.post("/logout", csrfProtector, Logout.class);

            app.get("/ontology", authenticator, csrfProtector, ReadPrivilegeFilter.class, Ontology.class);

            app.get("/notification/all", authenticator, csrfProtector, ReadPrivilegeFilter.class, Notifications.class);
            app.post("/notification/mark-read", authenticator, csrfProtector, ReadPrivilegeFilter.class, UserNotificationMarkRead.class);
            app.post("/notification/system", authenticator, csrfProtector, AdminPrivilegeFilter.class, SystemNotificationSave.class);
            app.delete("/notification/system", authenticator, csrfProtector, AdminPrivilegeFilter.class, SystemNotificationDelete.class);

            app.get("/resource", authenticator, csrfProtector, ReadPrivilegeFilter.class, ResourceGet.class);
            app.get("/resource/external", authenticator, csrfProtector, ReadPrivilegeFilter.class, ResourceExternalGet.class);
            app.get("/map/marker/image", csrfProtector, MapMarkerImage.class);  // TODO combine with /resource
            if (!(geocoderRepository instanceof DefaultGeocoderRepository)) {
                configuration.set(org.visallo.core.config.Configuration.WEB_GEOCODER_ENABLED, true);
                app.get("/map/geocode", authenticator, GetGeocoder.class);
            }
            if (configuration.get(org.visallo.core.config.Configuration.MAPZEN_TILE_API_KEY, null) == null) {
                LOGGER.warn("MapZen api key not found: %s", org.visallo.core.config.Configuration.MAPZEN_TILE_API_KEY);
            }
            app.get("/mapzen/{mapzenUri*}", authenticator, MapzenTileProxy.class);

            app.post("/search/save", authenticator, csrfProtector, SearchSave.class);
            app.get("/search/all", authenticator, csrfProtector, SearchList.class);
            app.get("/search/run", authenticator, csrfProtector, SearchRun.class);
            app.post("/search/run", authenticator, csrfProtector, SearchRun.class);
            app.delete("/search", authenticator, csrfProtector, SearchDelete.class);

            app.get("/element/search", authenticator, csrfProtector, ReadPrivilegeFilter.class, ElementSearch.class);
            app.post("/element/search", authenticator, csrfProtector, ReadPrivilegeFilter.class, ElementSearch.class);

            app.delete("/vertex", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexRemove.class);
            app.get("/vertex/highlighted-text", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexHighlightedText.class);
            app.get("/vertex/raw", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexRaw.class);
            app.get("/vertex/exists", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexExists.class);
            app.post("/vertex/exists", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexExists.class);
            app.get("/vertex/thumbnail", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexThumbnail.class);
            app.get("/vertex/poster-frame", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexPosterFrame.class);
            app.get("/vertex/video-preview", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexVideoPreviewImage.class);
            app.get("/vertex/details", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexDetails.class);
            app.get("/vertex/property/details", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexPropertyDetails.class);
            app.post("/vertex/import", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexImport.class);
            app.post("/vertex/resolve-term", authenticator, csrfProtector, EditPrivilegeFilter.class, ResolveTermEntity.class);
            app.post("/vertex/unresolve-term", authenticator, csrfProtector, EditPrivilegeFilter.class, UnresolveTermEntity.class);
            app.post("/vertex/resolve-detected-object", authenticator, csrfProtector, EditPrivilegeFilter.class, ResolveDetectedObject.class);
            app.post("/vertex/unresolve-detected-object", authenticator, csrfProtector, EditPrivilegeFilter.class, UnresolveDetectedObject.class);
            app.get("/vertex/detected-objects", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexGetDetectedObjects.class);
            app.get("/vertex/property", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexGetPropertyValue.class);
            app.get("/vertex/property/history", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexGetPropertyHistory.class);
            app.post("/vertex/property", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexSetProperty.class);
            app.post("/vertex/comment", authenticator, csrfProtector, CommentPrivilegeFilter.class, VertexSetProperty.class);
            app.delete("/vertex/property", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexDeleteProperty.class);
            app.get("/vertex/term-mentions", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexGetTermMentions.class);
            app.post("/vertex/visibility", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexSetVisibility.class);
            app.get("/vertex/properties", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexProperties.class);
            app.get("/vertex/edges", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexEdges.class);
            app.post("/vertex/multiple", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexMultiple.class); // this is a post method to allow large data (ie data larger than would fit in the URL)
            app.post("/vertex/new", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexNew.class);
            app.get("/vertex/search", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexSearch.class);
            app.post("/vertex/search", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexSearch.class);
            app.get("/vertex/geo-search", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexGeoSearch.class);
            app.post("/vertex/upload-image", authenticator, csrfProtector, EditPrivilegeFilter.class, VertexUploadImage.class);
            app.get("/vertex/find-path", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexFindPath.class);
            app.post("/vertex/find-related", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexFindRelated.class);
            app.get("/vertex/counts-by-concept-type", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexGetCountsByConceptType.class);
            app.get("/vertex/count", authenticator, csrfProtector, ReadPrivilegeFilter.class, VertexGetCount.class);
            app.get("/vertex/acl", authenticator, csrfProtector, ReadPrivilegeFilter.class, ElementGetAcl.class);

            app.post("/edge/property", authenticator, csrfProtector, EditPrivilegeFilter.class, SetEdgeProperty.class);
            app.post("/edge/comment", authenticator, csrfProtector, CommentPrivilegeFilter.class, SetEdgeProperty.class);
            app.delete("/edge", authenticator, csrfProtector, EditPrivilegeFilter.class, EdgeDelete.class);
            app.delete("/edge/property", authenticator, csrfProtector, EditPrivilegeFilter.class, DeleteEdgeProperty.class);
            app.get("/edge/property/history", authenticator, csrfProtector, ReadPrivilegeFilter.class, EdgeGetPropertyHistory.class);
            app.get("/edge/exists", authenticator, csrfProtector, ReadPrivilegeFilter.class, EdgeExists.class);
            app.post("/edge/exists", authenticator, csrfProtector, ReadPrivilegeFilter.class, EdgeExists.class);
            app.post("/edge/multiple", authenticator, csrfProtector, ReadPrivilegeFilter.class, EdgeMultiple.class);
            app.post("/edge/create", authenticator, csrfProtector, EditPrivilegeFilter.class, EdgeCreate.class);
            app.get("/edge/properties", authenticator, csrfProtector, ReadPrivilegeFilter.class, EdgeProperties.class);
            app.post("/edge/visibility", authenticator, csrfProtector, EditPrivilegeFilter.class, EdgeSetVisibility.class);
            app.get("/edge/property/details", authenticator, csrfProtector, ReadPrivilegeFilter.class, EdgePropertyDetails.class);
            app.get("/edge/details", authenticator, csrfProtector, ReadPrivilegeFilter.class, EdgeDetails.class);
            app.get("/edge/count", authenticator, csrfProtector, ReadPrivilegeFilter.class, EdgeGetCount.class);
            app.get("/edge/search", authenticator, csrfProtector, ReadPrivilegeFilter.class, EdgeSearch.class);
            app.post("/edge/search", authenticator, csrfProtector, ReadPrivilegeFilter.class, EdgeSearch.class);
            app.get("/edge/acl", authenticator, csrfProtector, ReadPrivilegeFilter.class, ElementGetAcl.class);

            app.get("/workspace/all", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceList.class);
            app.post("/workspace/create", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceCreate.class);
            app.get("/workspace/diff", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceDiff.class);
            app.get("/workspace/edges", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceEdges.class);
            app.post("/workspace/edges", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceEdges.class); // this is a post method to allow large data (ie data larger than would fit in the URL)
            app.get("/workspace/vertices", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceVertices.class);
            app.post("/workspace/update", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceUpdate.class);
            app.get("/workspace", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceById.class);
            app.delete("/workspace", authenticator, csrfProtector, ReadPrivilegeFilter.class, WorkspaceDelete.class);
            app.post("/workspace/publish", authenticator, csrfProtector, PublishPrivilegeFilter.class, WorkspacePublish.class);
            app.post("/workspace/undo", authenticator, csrfProtector, EditPrivilegeFilter.class, WorkspaceUndo.class);

            app.get("/user/me", authenticator, csrfProtector, MeGet.class);
            app.post("/user/ui-preferences", authenticator, csrfProtector, UserSetUiPreferences.class);
            app.get("/user/all", authenticator, csrfProtector, UserList.class);
            app.post("/user/all", authenticator, csrfProtector, UserList.class);
            app.get("/user", authenticator, csrfProtector, AdminPrivilegeFilter.class, UserGet.class);

            app.get("/long-running-process", authenticator, csrfProtector, LongRunningProcessById.class);
            app.delete("/long-running-process", authenticator, csrfProtector, LongRunningProcessDelete.class);
            app.post("/long-running-process/cancel", authenticator, csrfProtector, LongRunningProcessCancel.class);

            app.get("/admin/all", authenticator, csrfProtector, AdminPrivilegeFilter.class, AdminList.class);
            app.get("/admin/plugins", authenticator, csrfProtector, PluginList.class);
            app.post("/admin/upload-ontology", authenticator, csrfProtector, AdminPrivilegeFilter.class, AdminUploadOntology.class);

            app.get("/ping", RateLimitFilter.class, Ping.class);
            app.get("/ping/stats", authenticator, AdminPrivilegeFilter.class, PingStats.class);

            List<WebAppPlugin> webAppPlugins = toList(ServiceLoaderUtil.load(WebAppPlugin.class, configuration));
            for (WebAppPlugin webAppPlugin : webAppPlugins) {
                LOGGER.info("Loading webapp plugin: %s", webAppPlugin.getClass().getName());
                try {
                    webAppPlugin.init(app, servletContext, authenticatorInstance);
                } catch (Exception e) {
                    throw new VisalloException("Could not initialize webapp plugin: " + webAppPlugin.getClass().getName(), e);
                }
            }

            app.get("/css/images/ui-icons_222222_256x240.png",
                    new StaticResourceHandler(
                            this.getClass(),
                            "/org/visallo/web/routes/resource/ui-icons_222222_256x240.png",
                            "image/png")
            );

            app.onException(VisalloAccessDeniedException.class, new ErrorCodeHandler(HttpServletResponse.SC_FORBIDDEN));
        } catch (Exception ex) {
            LOGGER.error("Failed to initialize Router", ex);
            throw new RuntimeException("Failed to initialize " + getClass().getName(), ex);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        TraceSpan trace = null;
        CurrentUser.setUserInLogMappedDiagnosticContexts(request);
        try {
            if (request.getContentType() != null && request.getContentType().startsWith("multipart/form-data")) {
                request.setAttribute(JETTY_MULTIPART_CONFIG_ELEMENT8, MULTI_PART_CONFIG);
                request.setAttribute(JETTY_MULTIPART_CONFIG_ELEMENT9, MULTI_PART_CONFIG);
            }

            if (isGraphTraceEnabled(request)) {
                String traceDescription = request.getRequestURI();
                Map<String, String> parameters = new HashMap<>();
                for (Map.Entry<String, String[]> reqParameters : request.getParameterMap().entrySet()) {
                    parameters.put(reqParameters.getKey(), Joiner.on(", ").join(reqParameters.getValue()));
                }
                trace = Trace.on(traceDescription, parameters);
            }

            response.addHeader("Accept-Ranges", "bytes");
            app.handle(request, response);
        } catch (ConnectionClosedException cce) {
            LOGGER.debug("Connection closed by client", cce);
        } catch (Exception e) {
            if (e.getCause() instanceof VisalloResourceNotFoundException) {
                handleNotFound(response, (VisalloResourceNotFoundException) e.getCause());
                return;
            }
            if (e.getCause() instanceof BadRequestException) {
                handleBadRequest(response, (BadRequestException) e.getCause());
                return;
            }
            if (e.getCause() instanceof VisalloAccessDeniedException) {
                handleAccessDenied(response, (VisalloAccessDeniedException) e.getCause());
                return;
            }
            throw new ServletException(e);
        } finally {
            if (trace != null) {
                trace.close();
            }
            Trace.off();
            CurrentUser.clearUserFromLogMappedDiagnosticContexts();
        }
    }

    private void handleAccessDenied(HttpServletResponse response, VisalloAccessDeniedException accessDenied) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, accessDenied.getMessage());
    }

    private void handleNotFound(HttpServletResponse response, VisalloResourceNotFoundException notFoundException) throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, notFoundException.getMessage());
    }

    private void handleBadRequest(HttpServletResponse response, BadRequestException badRequestException) {
        LOGGER.error("bad request", badRequestException);
        JSONObject error = new JSONObject();
        error.put(badRequestException.getParameterName(), badRequestException.getMessage());
        if (badRequestException.getInvalidValues() != null) {
            JSONArray values = new JSONArray();
            for (String v : badRequestException.getInvalidValues()) {
                values.put(v);
            }
            error.put("invalidValues", values);
        }
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        VisalloResponse.configureResponse(ResponseTypes.JSON_OBJECT, response, error);
    }

    private boolean isGraphTraceEnabled(ServletRequest req) {
        return req.getParameter(GRAPH_TRACE_ENABLE) != null || req instanceof HttpServletRequest && ((HttpServletRequest) req).getHeader(GRAPH_TRACE_ENABLE) != null;
    }

    @Inject
    public void setConfiguration(org.visallo.core.config.Configuration configuration) {
        this.configuration = configuration;
    }

    @Inject
    public void setGeocoderRepository(GeocoderRepository geocoderRepository) {
        this.geocoderRepository = geocoderRepository;
    }

    public WebApp getApp() {
        return app;
    }
}
