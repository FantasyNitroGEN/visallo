package org.visallo.core.ingest.graphProperty;

import com.google.inject.Injector;
import org.vertexium.Authorizations;
import org.visallo.core.user.User;

import java.util.List;
import java.util.Map;

public class GraphPropertyWorkerPrepareData {
    private final Map configuration;
    private final List<TermMentionFilter> termMentionFilters;
    private final User user;
    private final Authorizations authorizations;
    private final Injector injector;

    public GraphPropertyWorkerPrepareData(Map configuration, List<TermMentionFilter> termMentionFilters, User user, Authorizations authorizations, Injector injector) {
        this.configuration = configuration;
        this.termMentionFilters = termMentionFilters;
        this.user = user;
        this.authorizations = authorizations;
        this.injector = injector;
    }

    public Iterable<TermMentionFilter> getTermMentionFilters() {
        return termMentionFilters;
    }

    public Map getConfiguration() {
        return configuration;
    }

    public User getUser() {
        return user;
    }

    public Authorizations getAuthorizations() {
        return authorizations;
    }

    public Injector getInjector() {
        return injector;
    }
}
