package org.visallo.test;

import com.google.inject.Injector;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.vertexium.*;
import org.vertexium.id.IdGenerator;
import org.vertexium.id.QueueIdGenerator;
import org.vertexium.inmemory.InMemoryGraph;
import org.vertexium.inmemory.InMemoryGraphConfiguration;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.search.DefaultSearchIndex;
import org.vertexium.search.SearchIndex;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.ConfigurationLoader;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.graphProperty.*;
import org.visallo.core.model.WorkQueueNames;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.DirectVisibilityTranslator;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.model.queue.inmemory.InMemoryWorkQueueRepository;
import org.visallo.vertexium.model.user.InMemoryUser;
import org.visallo.web.clientapi.model.Privilege;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public abstract class GraphPropertyWorkerTestBase {
    private InMemoryGraph graph;
    private IdGenerator graphIdGenerator;
    private SearchIndex graphSearchIndex;
    private HashMap<String, String> configurationMap;
    private GraphPropertyWorkerPrepareData graphPropertyWorkerPrepareData;
    private User user;
    private WorkQueueNames workQueueNames;
    private WorkQueueRepository workQueueRepository;
    private VisibilityTranslator visibilityTranslator = new DirectVisibilityTranslator();

    @Mock
    protected OntologyRepository ontologyRepository;

    protected GraphPropertyWorkerTestBase() {

    }

    @Before
    public final void clearGraph() {
        if (graph != null) {
            graph.shutdown();
            graph = null;
        }
        graphIdGenerator = null;
        graphSearchIndex = null;
        configurationMap = null;
        graphPropertyWorkerPrepareData = null;
        user = null;
        workQueueRepository = null;
        System.setProperty(ConfigurationLoader.ENV_CONFIGURATION_LOADER, HashMapConfigurationLoader.class.getName());
        workQueueNames = new WorkQueueNames(getConfiguration());

        InMemoryWorkQueueRepository.clearQueue();
    }

    @After
    public final void after() {
        clearGraph();
    }

    protected GraphPropertyWorkerPrepareData getWorkerPrepareData() {
        return getWorkerPrepareData(null, null, null, null, null);
    }

    protected GraphPropertyWorkerPrepareData getWorkerPrepareData(Map configuration, List<TermMentionFilter> termMentionFilters, User user, Authorizations authorizations, Injector injector) {
        if (graphPropertyWorkerPrepareData == null) {
            if (configuration == null) {
                configuration = getConfigurationMap();
            }
            if (termMentionFilters == null) {
                termMentionFilters = new ArrayList<>();
            }
            if (user == null) {
                user = getUser();
            }
            if (authorizations == null) {
                authorizations = getGraphAuthorizations(VisalloVisibility.SUPER_USER_VISIBILITY_STRING);
            }
            graphPropertyWorkerPrepareData = new GraphPropertyWorkerPrepareData(configuration, termMentionFilters, user, authorizations, injector);
        }
        return graphPropertyWorkerPrepareData;
    }

    protected User getUser() {
        if (user == null) {
            user = new InMemoryUser("test", "Test User", "test@visallo.org", null);
        }
        return user;
    }

    protected Graph getGraph() {
        if (graph == null) {
            Map graphConfiguration = getConfigurationMap();
            InMemoryGraphConfiguration inMemoryGraphConfiguration = new InMemoryGraphConfiguration(graphConfiguration);
            graph = InMemoryGraph.create(inMemoryGraphConfiguration, getGraphIdGenerator(), getGraphSearchIndex(inMemoryGraphConfiguration));
        }
        return graph;
    }

    protected IdGenerator getGraphIdGenerator() {
        if (graphIdGenerator == null) {
            graphIdGenerator = new QueueIdGenerator();
        }
        return graphIdGenerator;
    }

    protected SearchIndex getGraphSearchIndex(GraphConfiguration inMemoryGraphConfiguration) {
        if (graphSearchIndex == null) {
            graphSearchIndex = new DefaultSearchIndex(inMemoryGraphConfiguration);
        }
        return graphSearchIndex;
    }

    protected Map getConfigurationMap() {
        if (configurationMap == null) {
            configurationMap = new HashMap<>();
            configurationMap.put("ontology.intent.concept.location", "http://visallo.org/test#location");
            configurationMap.put("ontology.intent.concept.organization", "http://visallo.org/test#organization");
            configurationMap.put("ontology.intent.concept.person", "http://visallo.org/test#person");
        }
        return configurationMap;
    }

    protected Authorizations getGraphAuthorizations(String... authorizations) {
        return getGraph().createAuthorizations(authorizations);
    }

    protected byte[] getResourceAsByteArray(Class sourceClass, String resourceName) {
        try {
            InputStream in = sourceClass.getResourceAsStream(resourceName);
            if (in == null) {
                throw new IOException("Could not find resource: " + resourceName);
            }
            return IOUtils.toByteArray(in);
        } catch (IOException e) {
            throw new VisalloException("Could not load resource. " + sourceClass.getName() + " at " + resourceName, e);
        }
    }

    protected void prepare(GraphPropertyWorker gpw) throws Exception {
        gpw.setOntologyRepository(ontologyRepository);
        gpw.setVisibilityTranslator(getVisibilityTranslator());
        gpw.setGraph(getGraph());
        gpw.setWorkQueueRepository(getWorkQueueRepository());
        gpw.prepare(getWorkerPrepareData());
    }

    protected void run(GraphPropertyWorker gpw, GraphPropertyWorkerPrepareData workerPrepareData, Element e) {
        for (Property property : e.getProperties()) {
            InputStream in = null;
            if (property.getValue() instanceof StreamingPropertyValue) {
                StreamingPropertyValue spv = (StreamingPropertyValue) property.getValue();
                in = spv.getInputStream();
            }
            run(gpw, workerPrepareData, e, property, in);
        }
    }

    protected boolean run(GraphPropertyWorker gpw, GraphPropertyWorkerPrepareData workerPrepareData, Element e, Property prop, InputStream in) {
        return run(gpw, workerPrepareData, e, prop, in, null, null, null);
    }

    protected boolean run(GraphPropertyWorker gpw, GraphPropertyWorkerPrepareData workerPrepareData, Element e, Property prop, InputStream in, ElementOrPropertyStatus status) {
        return run(gpw, workerPrepareData, e, prop, in, null, status, null);
    }

    protected boolean run(
            GraphPropertyWorker gpw,
            GraphPropertyWorkerPrepareData workerPrepareData,
            Element e,
            Property prop,
            InputStream in,
            String workspaceId,
            ElementOrPropertyStatus status,
            String visibilitySource) {
        try {
            gpw.setConfiguration(getConfiguration());
            gpw.setGraph(getGraph());
            gpw.setVisibilityTranslator(getVisibilityTranslator());
            gpw.setWorkQueueRepository(getWorkQueueRepository());
            gpw.prepare(workerPrepareData);
        } catch (Exception ex) {
            throw new VisalloException("Failed to prepare: " + gpw.getClass().getName(), ex);
        }

        try {
            if (!gpw.isHandled(e, prop)) {
                return false;
            }
        } catch (Exception ex) {
            throw new VisalloException("Failed to isHandled: " + gpw.getClass().getName(), ex);
        }

        try {
            GraphPropertyWorkData executeData = new GraphPropertyWorkData(
                    visibilityTranslator,
                    e,
                    prop,
                    workspaceId,
                    visibilitySource,
                    Priority.NORMAL,
                    prop.getTimestamp() - 1,
                    status
            );
            if (gpw.isLocalFileRequired() && executeData.getLocalFile() == null && in != null) {
                byte[] data = IOUtils.toByteArray(in);
                File tempFile = File.createTempFile("visalloTest", "data");
                FileUtils.writeByteArrayToFile(tempFile, data);
                executeData.setLocalFile(tempFile);
                in = new ByteArrayInputStream(data);
            }
            gpw.execute(in, executeData);
        } catch (Exception ex) {
            throw new VisalloException("Failed to execute: " + gpw.getClass().getName(), ex);
        }
        return true;
    }

    protected WorkQueueRepository getWorkQueueRepository() {
        if (workQueueRepository == null) {
            workQueueRepository = new InMemoryWorkQueueRepository(getGraph(), workQueueNames, getConfiguration());
        }
        return workQueueRepository;
    }

    protected List<JSONObject> getGraphPropertyQueue() {
        return InMemoryWorkQueueRepository.getQueue(workQueueNames.getGraphPropertyQueueName());
    }

    protected VisibilityTranslator getVisibilityTranslator() {
        return visibilityTranslator;
    }

    protected Configuration getConfiguration() {
        return new HashMapConfigurationLoader(getConfigurationMap()).createConfiguration();
    }
}
