package org.visallo.web.routes.element;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.json.JSONArray;
import org.vertexium.Authorizations;
import org.vertexium.ElementType;
import org.vertexium.Graph;
import org.vertexium.query.Query;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiElementSearchResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.VisalloBaseParameterProvider;
import org.visallo.web.routes.vertex.ElementSearchBase;

import javax.servlet.http.HttpServletRequest;
import java.util.EnumSet;

public class ElementSearch extends ElementSearchBase implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ElementSearch.class);

    @Inject
    public ElementSearch(
            OntologyRepository ontologyRepository,
            Graph graph,
            Configuration configuration
    ) {
        super(ontologyRepository, graph, configuration);
    }

    @Override
    protected EnumSet<ElementType> getResultType() {
        return EnumSet.of(ElementType.EDGE, ElementType.VERTEX);
    }

    @Override
    @Handle
    public ClientApiElementSearchResponse handle(
            HttpServletRequest request,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        return super.handle(request, workspaceId, authorizations);
    }

    @Override
    protected QueryAndData getQuery(HttpServletRequest request, final Authorizations authorizations) {
        final JSONArray filterJson = getFilterJson(request);
        final String queryString = VisalloBaseParameterProvider.getRequiredParameter(request, "q");
        LOGGER.debug("search %s\n%s", queryString, filterJson.toString(2));

        Query graphQuery = query(queryString, authorizations);

        return new QueryAndData(graphQuery);
    }

    private Query query(String query, Authorizations authorizations) {
        return getGraph().query(query, authorizations);
    }
}
