package org.visallo.core.model.search;

import org.mockito.Mock;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Visibility;
import org.vertexium.inmemory.InMemoryGraph;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.ConfigurationLoader;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.model.directory.DirectoryRepository;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.user.User;

import java.util.HashMap;
import java.util.Map;

public abstract class SearchRunnerTestBase {
    protected Visibility visibility;
    protected Authorizations authorizations;
    protected Graph graph;
    protected Configuration configuration;

    @Mock
    protected DirectoryRepository directoryRepository;

    @Mock
    protected SearchRepository searchRepository;

    @Mock
    protected OntologyRepository ontologyRepository;

    @Mock
    protected User user;

    public void before() {
        Map config = new HashMap();
        ConfigurationLoader hashMapConfigurationLoader = new HashMapConfigurationLoader(config);
        configuration = new Configuration(hashMapConfigurationLoader, new HashMap<>());

        graph = InMemoryGraph.create(getGraphConfiguration());
        visibility = new Visibility("");
        authorizations = graph.createAuthorizations("");
    }

    protected Map<String, Object> getGraphConfiguration() {
        return new HashMap<>();
    }
}
