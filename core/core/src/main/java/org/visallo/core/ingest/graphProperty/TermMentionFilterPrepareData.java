package org.visallo.core.ingest.graphProperty;

import com.google.inject.Injector;
import org.vertexium.Authorizations;
import org.visallo.core.user.User;

import java.util.Map;

public class TermMentionFilterPrepareData {
    private final Map configuration;
    private final User user;
    private final Authorizations authorizations;
    private final Injector injector;

    public TermMentionFilterPrepareData(Map configuration, User user, Authorizations authorizations, Injector injector) {
        this.configuration = configuration;
        this.user = user;
        this.authorizations = authorizations;
        this.injector = injector;
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
