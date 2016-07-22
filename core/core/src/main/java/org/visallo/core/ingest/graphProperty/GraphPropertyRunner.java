package org.visallo.core.ingest.graphProperty;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.vertexium.*;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.util.IterableUtils;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.WorkQueueNames;
import org.visallo.core.model.WorkerBase;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.status.StatusRepository;
import org.visallo.core.status.StatusServer;
import org.visallo.core.status.model.GraphPropertyRunnerStatus;
import org.visallo.core.status.model.ProcessStatus;
import org.visallo.core.user.User;
import org.visallo.core.util.ServiceLoaderUtil;
import org.visallo.core.util.TeeInputStream;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.vertexium.util.IterableUtils.toList;

@Singleton
public class GraphPropertyRunner extends WorkerBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GraphPropertyRunner.class);
    private final StatusRepository statusRepository;
    private final AuthorizationRepository authorizationRepository;
    private Graph graph;
    private Authorizations authorizations;
    private List<GraphPropertyThreadedWrapper> workerWrappers = Lists.newArrayList();
    private User user;
    private UserRepository userRepository;
    private WorkQueueNames workQueueNames;
    private Configuration configuration;
    private VisibilityTranslator visibilityTranslator;
    private AtomicLong lastProcessedPropertyTime = new AtomicLong(0);
    private List<GraphPropertyWorker> graphPropertyWorkers = Lists.newArrayList();

    @Inject
    protected GraphPropertyRunner(
            WorkQueueRepository workQueueRepository,
            StatusRepository statusRepository,
            Configuration configuration,
            AuthorizationRepository authorizationRepository
    ) {
        super(workQueueRepository, configuration);
        this.statusRepository = statusRepository;
        this.authorizationRepository = authorizationRepository;
    }

    @Override
    public void process(Object messageId, JSONObject json) throws Exception {
        GraphPropertyMessage message = new GraphPropertyMessage(json);
        if (!message.isValid()) {
            throw new VisalloException(String.format("Cannot process unknown type of gpw message %s", json.toString()));
        } else if (message.canHandleByProperty()) {
            safeExecuteHandlePropertyOnElements(message);
        } else {
            safeExecuteHandleAllEntireElements(message);
        }
    }

    public void prepare(User user) {
        prepare(user, new GraphPropertyWorkerInitializer());
    }

    public void prepare(User user, GraphPropertyWorkerInitializer repository) {
        setUser(user);
        setAuthorizations(authorizationRepository.getGraphAuthorizations(user));
        prepareWorkers(repository);
        this.getWorkQueueRepository().setGraphPropertyRunner(this);
    }

    public void prepareWorkers(GraphPropertyWorkerInitializer initializer) {
        List<TermMentionFilter> termMentionFilters = loadTermMentionFilters();

        GraphPropertyWorkerPrepareData workerPrepareData = new GraphPropertyWorkerPrepareData(
                configuration.toMap(),
                termMentionFilters,
                this.user,
                this.authorizations,
                InjectHelper.getInjector()
        );
        Collection<GraphPropertyWorker> workers = InjectHelper.getInjectedServices(
                GraphPropertyWorker.class,
                configuration
        );
        for (GraphPropertyWorker worker : workers) {
            try {
                LOGGER.debug("verifying: %s", worker.getClass().getName());
                VerifyResults verifyResults = worker.verify();
                if (verifyResults != null && verifyResults.getFailures().size() > 0) {
                    LOGGER.error("graph property worker %s had errors verifying", worker.getClass().getName());
                    for (VerifyResults.Failure failure : verifyResults.getFailures()) {
                        LOGGER.error("  %s", failure.getMessage());
                    }
                }

                if (initializer != null) {
                    initializer.initialize(worker);
                }
            } catch (Exception ex) {
                LOGGER.error("Could not verify graph property worker %s", worker.getClass().getName(), ex);
            }
        }

        boolean failedToPrepareAtLeastOneGraphPropertyWorker = false;
        List<GraphPropertyThreadedWrapper> wrappers = Lists.newArrayList();
        for (GraphPropertyWorker worker : workers) {
            try {
                LOGGER.debug("preparing: %s", worker.getClass().getName());
                worker.prepare(workerPrepareData);
            } catch (Exception ex) {
                LOGGER.error("Could not prepare graph property worker %s", worker.getClass().getName(), ex);
                failedToPrepareAtLeastOneGraphPropertyWorker = true;
            }

            GraphPropertyThreadedWrapper wrapper = new GraphPropertyThreadedWrapper(worker);
            InjectHelper.inject(wrapper);
            wrappers.add(wrapper);
            Thread thread = new Thread(wrapper);
            String workerName = worker.getClass().getName();
            thread.setName("graphPropertyWorker-" + workerName);
            thread.start();
        }

        this.addGraphPropertyThreadedWrappers(wrappers);
        this.graphPropertyWorkers.addAll(workers);

        if (failedToPrepareAtLeastOneGraphPropertyWorker) {
            throw new VisalloException(
                    "Failed to initialize at least one graph property worker. See the log for more details.");
        }
    }

    public void addGraphPropertyThreadedWrappers(List<GraphPropertyThreadedWrapper> wrappers) {
        this.workerWrappers.addAll(wrappers);
    }

    public void addGraphPropertyThreadedWrappers(GraphPropertyThreadedWrapper... wrappers) {
        this.workerWrappers.addAll(Lists.newArrayList(wrappers));
    }

    private List<TermMentionFilter> loadTermMentionFilters() {
        TermMentionFilterPrepareData termMentionFilterPrepareData = new TermMentionFilterPrepareData(
                configuration.toMap(),
                this.user,
                this.authorizations,
                InjectHelper.getInjector()
        );

        List<TermMentionFilter> termMentionFilters = toList(ServiceLoaderUtil.load(
                TermMentionFilter.class,
                configuration
        ));
        for (TermMentionFilter termMentionFilter : termMentionFilters) {
            try {
                termMentionFilter.prepare(termMentionFilterPrepareData);
            } catch (Exception ex) {
                throw new VisalloException(
                        "Could not initialize term mention filter: " + termMentionFilter.getClass().getName(),
                        ex
                );
            }
        }
        return termMentionFilters;
    }

    @Override
    protected StatusServer createStatusServer() throws Exception {
        return new StatusServer(configuration, statusRepository, "graphProperty", GraphPropertyRunner.class) {
            @Override
            protected ProcessStatus createStatus() {
                GraphPropertyRunnerStatus status = new GraphPropertyRunnerStatus();
                for (GraphPropertyThreadedWrapper graphPropertyThreadedWrapper : workerWrappers) {
                    status.getRunningWorkers().add(graphPropertyThreadedWrapper.getStatus());
                }
                return status;
            }
        };
    }

    private void safeExecuteHandleAllEntireElements(GraphPropertyMessage message) throws Exception {
        List<Element> elements = getElement(message);
        for (Element element : elements) {
            safeExecuteHandleEntireElement(element, message);
        }
    }

    private void safeExecuteHandleEntireElement(Element element, GraphPropertyMessage message) throws Exception {
        safeExecuteHandlePropertyOnElement(element, null, message);
        for (Property property : element.getProperties()) {
            safeExecuteHandlePropertyOnElement(element, property, message);
        }
    }

    private List<Element> getVerticesFromMessage(GraphPropertyMessage message) {
        List<Element> vertices = Lists.newLinkedList();

        for (String vertexId : message.getVertexIds()) {
            Vertex vertex;
            if (message.getStatus() == ElementOrPropertyStatus.DELETION || message.getStatus() == ElementOrPropertyStatus.HIDDEN) {
                vertex = graph.getVertex(
                        vertexId,
                        FetchHint.ALL,
                        message.getBeforeActionTimestamp(),
                        this.authorizations
                );
            } else {
                vertex = graph.getVertex(vertexId, this.authorizations);
            }
            if (doesExist(vertex)) {
                vertices.add(vertex);
            } else {
                LOGGER.warn("Could not find vertex with id %s", vertexId);
            }
        }
        return vertices;
    }

    private List<Element> getEdgesFromMessage(GraphPropertyMessage message) {
        List<Element> edges = Lists.newLinkedList();

        for (String edgeId : message.getEdgeIds()) {
            Edge edge;
            if (message.getStatus() == ElementOrPropertyStatus.DELETION || message.getStatus() == ElementOrPropertyStatus.HIDDEN) {
                edge = graph.getEdge(edgeId, FetchHint.ALL, message.getBeforeActionTimestamp(), this.authorizations);
            } else {
                edge = graph.getEdge(edgeId, this.authorizations);
            }
            if (doesExist(edge)) {
                edges.add(edge);
            } else {
                LOGGER.warn("Could not find edge with id %s", edgeId);
            }
        }
        return edges;
    }

    private boolean doesExist(Element element) {
        return element != null;
    }

    private void safeExecuteHandlePropertyOnElements(GraphPropertyMessage message) throws Exception {
        List<Element> elements = getElement(message);
        for (Element element : elements) {
            Property property = null;
            if (StringUtils.isNotEmpty(message.getPropertyKey()) || StringUtils.isNotEmpty(message.getPropertyName())) {
                if (message.getPropertyKey() == null) {
                    property = element.getProperty(message.getPropertyName());
                } else {
                    property = element.getProperty(message.getPropertyKey(), message.getPropertyName());
                }

                if (property == null) {
                    LOGGER.error(
                            "Could not find property [%s]:[%s] on vertex with id %s",
                            message.getPropertyKey(),
                            message.getPropertyName(),
                            element.getId()
                    );
                    continue;
                }
            }

            safeExecuteHandlePropertyOnElement(element, property, message);
        }
    }

    private void safeExecuteHandlePropertyOnElement(
            Element element,
            Property property,
            GraphPropertyMessage message
    ) throws Exception {
        String propertyText = getPropertyText(property);
        ElementOrPropertyStatus status = message.getStatus();

        List<GraphPropertyThreadedWrapper> interestedWorkerWrappers = findInterestedWorkers(element, property, status);
        if (interestedWorkerWrappers.size() == 0) {
            LOGGER.debug(
                    "Could not find interested workers for %s %s property %s (%s)",
                    element instanceof Vertex ? "vertex" : "edge",
                    element.getId(),
                    propertyText,
                    status
            );
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            for (GraphPropertyThreadedWrapper interestedWorkerWrapper : interestedWorkerWrappers) {
                LOGGER.debug(
                        "interested worker for %s %s property %s: %s (%s)",
                        element instanceof Vertex ? "vertex" : "edge",
                        element.getId(),
                        propertyText,
                        interestedWorkerWrapper.getWorker().getClass().getName(),
                        status
                );
            }
        }

        GraphPropertyWorkData workData = new GraphPropertyWorkData(
                visibilityTranslator,
                element,
                property,
                message.getWorkspaceId(),
                message.getVisibilitySource(),
                message.getPriority(),
                message.getBeforeActionTimestamp(),
                status
        );

        LOGGER.debug("Begin work on element %s property %s", element.getId(), propertyText);
        if (property != null && property.getValue() instanceof StreamingPropertyValue) {
            StreamingPropertyValue spb = (StreamingPropertyValue) property.getValue();
            safeExecuteStreamingPropertyValue(interestedWorkerWrappers, workData, spb);
        } else {
            safeExecuteNonStreamingProperty(interestedWorkerWrappers, workData);
        }

        lastProcessedPropertyTime.set(System.currentTimeMillis());

        this.graph.flush();

        LOGGER.debug("Completed work on %s", propertyText);
    }

    private String getPropertyText(Property property) {
        return property == null ? "[none]" : (property.getKey() + ":" + property.getName());
    }

    private void safeExecuteNonStreamingProperty(
            List<GraphPropertyThreadedWrapper> interestedWorkerWrappers,
            GraphPropertyWorkData workData
    ) throws Exception {
        for (GraphPropertyThreadedWrapper interestedWorkerWrapper1 : interestedWorkerWrappers) {
            interestedWorkerWrapper1.enqueueWork(null, workData);
        }

        for (GraphPropertyThreadedWrapper interestedWorkerWrapper : interestedWorkerWrappers) {
            interestedWorkerWrapper.dequeueResult(true);
        }
    }

    private void safeExecuteStreamingPropertyValue(
            List<GraphPropertyThreadedWrapper> interestedWorkerWrappers,
            GraphPropertyWorkData workData,
            StreamingPropertyValue streamingPropertyValue
    ) throws Exception {
        String[] workerNames = graphPropertyThreadedWrapperToNames(interestedWorkerWrappers);
        InputStream in = streamingPropertyValue.getInputStream();
        File tempFile = null;
        try {
            boolean requiresLocalFile = isLocalFileRequired(interestedWorkerWrappers);
            if (requiresLocalFile) {
                tempFile = copyToTempFile(in, workData);
                in = new FileInputStream(tempFile);
            }

            TeeInputStream teeInputStream = new TeeInputStream(in, workerNames);
            for (int i = 0; i < interestedWorkerWrappers.size(); i++) {
                interestedWorkerWrappers.get(i).enqueueWork(teeInputStream.getTees()[i], workData);
            }
            teeInputStream.loopUntilTeesAreClosed();
            for (GraphPropertyThreadedWrapper interestedWorkerWrapper : interestedWorkerWrappers) {
                interestedWorkerWrapper.dequeueResult(false);
            }
        } finally {
            if (tempFile != null) {
                if (!tempFile.delete()) {
                    LOGGER.warn("Could not delete temp file %s", tempFile.getAbsolutePath());
                }
            }
        }
    }

    private File copyToTempFile(InputStream in, GraphPropertyWorkData workData) throws IOException {
        String fileExt = null;
        String fileName = VisalloProperties.FILE_NAME.getOnlyPropertyValue(workData.getElement());
        if (fileName != null) {
            fileExt = FilenameUtils.getExtension(fileName);
        }
        if (fileExt == null) {
            fileExt = "data";
        }
        File tempFile = File.createTempFile("graphPropertyBolt", fileExt);
        workData.setLocalFile(tempFile);
        try (OutputStream tempFileOut = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, tempFileOut);
        } finally {
            in.close();

        }
        return tempFile;
    }

    private boolean isLocalFileRequired(List<GraphPropertyThreadedWrapper> interestedWorkerWrappers) {
        for (GraphPropertyThreadedWrapper worker : interestedWorkerWrappers) {
            if (worker.getWorker().isLocalFileRequired()) {
                return true;
            }
        }
        return false;
    }

    private List<GraphPropertyThreadedWrapper> findInterestedWorkers(
            Element element,
            Property property,
            ElementOrPropertyStatus status
    ) {
        Set<String> graphPropertyWorkerWhiteList = IterableUtils.toSet(VisalloProperties.GRAPH_PROPERTY_WORKER_WHITE_LIST.getPropertyValues(
                element));
        Set<String> graphPropertyWorkerBlackList = IterableUtils.toSet(VisalloProperties.GRAPH_PROPERTY_WORKER_BLACK_LIST.getPropertyValues(
                element));

        List<GraphPropertyThreadedWrapper> interestedWorkers = new ArrayList<>();
        for (GraphPropertyThreadedWrapper wrapper : workerWrappers) {
            String graphPropertyWorkerName = wrapper.getWorker().getClass().getName();
            if (graphPropertyWorkerWhiteList.size() > 0 && !graphPropertyWorkerWhiteList.contains(
                    graphPropertyWorkerName)) {
                continue;
            }
            if (graphPropertyWorkerBlackList.contains(graphPropertyWorkerName)) {
                continue;
            }
            GraphPropertyWorker worker = wrapper.getWorker();
            if (status == ElementOrPropertyStatus.DELETION) {
                addDeletedWorkers(interestedWorkers, worker, wrapper, element, property);
            } else if (status == ElementOrPropertyStatus.HIDDEN) {
                addHiddenWorkers(interestedWorkers, worker, wrapper, element, property);
            } else if (status == ElementOrPropertyStatus.UNHIDDEN) {
                addUnhiddenWorkers(interestedWorkers, worker, wrapper, element, property);
            } else if (worker.isHandled(element, property)) {
                interestedWorkers.add(wrapper);
            }
        }

        return interestedWorkers;
    }

    private void addDeletedWorkers(
            List<GraphPropertyThreadedWrapper> interestedWorkers,
            GraphPropertyWorker worker,
            GraphPropertyThreadedWrapper wrapper,
            Element element,
            Property property
    ) {
        if (worker.isDeleteHandled(element, property)) {
            interestedWorkers.add(wrapper);
        }
    }

    private void addHiddenWorkers(
            List<GraphPropertyThreadedWrapper> interestedWorkers,
            GraphPropertyWorker worker,
            GraphPropertyThreadedWrapper wrapper,
            Element element,
            Property property
    ) {
        if (worker.isHiddenHandled(element, property)) {
            interestedWorkers.add(wrapper);
        }
    }

    private void addUnhiddenWorkers(
            List<GraphPropertyThreadedWrapper> interestedWorkers,
            GraphPropertyWorker worker,
            GraphPropertyThreadedWrapper wrapper,
            Element element,
            Property property
    ) {
        if (worker.isUnhiddenHandled(element, property)) {
            interestedWorkers.add(wrapper);
        }
    }

    private String[] graphPropertyThreadedWrapperToNames(List<GraphPropertyThreadedWrapper> interestedWorkerWrappers) {
        String[] names = new String[interestedWorkerWrappers.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = interestedWorkerWrappers.get(i).getWorker().getClass().getName();
        }
        return names;
    }

    private List<Element> getElement(GraphPropertyMessage message) {
        if (message.canHandleVertex()) {
            return getVerticesFromMessage(message);
        } else if (message.canHandleEdge()) {
            return getEdgesFromMessage(message);
        } else {
            throw new VisalloException(String.format(
                    "Could not find %s or %s",
                    GraphPropertyMessage.GRAPH_VERTEX_ID,
                    GraphPropertyMessage.GRAPH_EDGE_ID
            ));
        }
    }

    public void shutdown() {
        for (GraphPropertyThreadedWrapper wrapper : this.workerWrappers) {
            wrapper.stop();
        }

        super.stop();
    }

    public UserRepository getUserRepository() {
        return this.userRepository;
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Inject
    public void setWorkQueueNames(WorkQueueNames workQueueNames) {
        this.workQueueNames = workQueueNames;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Inject
    public void setVisibilityTranslator(VisibilityTranslator visibilityTranslator) {
        this.visibilityTranslator = visibilityTranslator;
    }


    public void setAuthorizations(Authorizations authorizations) {
        this.authorizations = authorizations;
    }

    public long getLastProcessedTime() {
        return this.lastProcessedPropertyTime.get();
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    protected String getQueueName() {
        return workQueueNames.getGraphPropertyQueueName();
    }

    public boolean isStarted() {
        return this.shouldRun();
    }

    public boolean canHandle(Element element, String propertyKey, String propertyName) {
        if (!this.isStarted()) {
            //we are probably on a server and want to submit it to the architecture
            return true;
        }

        Property property = element.getProperty(propertyKey, propertyName);

        for (GraphPropertyWorker worker : this.getAllGraphPropertyWorkers()) {
            try {
                if (worker.isHandled(element, property)) {
                    return true;
                } else if (worker.isDeleteHandled(element, property)) {
                    return true;
                } else if (worker.isHiddenHandled(element, property)) {
                    return true;
                } else if (worker.isUnhiddenHandled(element, property)) {
                    return true;
                }
            } catch (Throwable t) {
                LOGGER.warn(
                        "Error checking to see if workers will handle graph property message.  Queueing anyways in case there was just a local error",
                        t
                );
                return true;
            }
        }

        LOGGER.debug(
                "No interested workers for %s %s %s so did not queue it",
                element.getId(),
                propertyKey,
                propertyName
        );

        return false;
    }

    private Collection<GraphPropertyWorker> getAllGraphPropertyWorkers() {
        return Lists.newArrayList(this.graphPropertyWorkers);
    }
}
