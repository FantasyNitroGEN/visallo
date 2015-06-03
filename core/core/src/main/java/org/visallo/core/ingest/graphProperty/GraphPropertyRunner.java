package org.visallo.core.ingest.graphProperty;

import com.google.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
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
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.security.VisibilityTranslator;
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

import static org.vertexium.util.IterableUtils.toList;

public class GraphPropertyRunner extends WorkerBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GraphPropertyRunner.class);
    private Graph graph;
    private Authorizations authorizations;
    private List<GraphPropertyThreadedWrapper> workerWrappers;
    private User user;
    private UserRepository userRepository;
    private WorkQueueNames workQueueNames;
    private Configuration configuration;
    private VisibilityTranslator visibilityTranslator;

    public void prepare(User user) {
        this.user = user;
        this.authorizations = this.userRepository.getAuthorizations(this.user);
        prepareWorkers();
    }

    private void prepareWorkers() {
        FileSystem hdfsFileSystem = configuration.getFileSystem();

        List<TermMentionFilter> termMentionFilters = loadTermMentionFilters(hdfsFileSystem);

        GraphPropertyWorkerPrepareData workerPrepareData = new GraphPropertyWorkerPrepareData(
                configuration.toMap(),
                termMentionFilters,
                hdfsFileSystem,
                this.user,
                this.authorizations,
                InjectHelper.getInjector());
        Collection<GraphPropertyWorker> workers = InjectHelper.getInjectedServices(GraphPropertyWorker.class, configuration);
        this.workerWrappers = new ArrayList<>(workers.size());
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
            } catch (Exception ex) {
                LOGGER.error("Could not verify graph property worker %s", worker.getClass().getName(), ex);
            }
        }

        boolean failedToPrepareAtLeastOneGraphPropertyWorker = false;
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
            workerWrappers.add(wrapper);
            Thread thread = new Thread(wrapper);
            String workerName = worker.getClass().getName();
            thread.setName("graphPropertyWorker-" + workerName);
            thread.start();
        }
        if (failedToPrepareAtLeastOneGraphPropertyWorker) {
            throw new VisalloException("Failed to initialize at least one graph property worker. See the log for more details.");
        }
    }

    private List<TermMentionFilter> loadTermMentionFilters(FileSystem hdfsFileSystem) {
        TermMentionFilterPrepareData termMentionFilterPrepareData = new TermMentionFilterPrepareData(
                configuration.toMap(),
                hdfsFileSystem,
                this.user,
                this.authorizations,
                InjectHelper.getInjector()
        );

        List<TermMentionFilter> termMentionFilters = toList(ServiceLoaderUtil.load(TermMentionFilter.class, configuration));
        for (TermMentionFilter termMentionFilter : termMentionFilters) {
            InjectHelper.inject(termMentionFilter);
            try {
                termMentionFilter.prepare(termMentionFilterPrepareData);
            } catch (Exception ex) {
                throw new VisalloException("Could not initialize term mention filter: " + termMentionFilter.getClass().getName(), ex);
            }
        }
        return termMentionFilters;
    }

    @Override
    protected StatusServer createStatusServer() throws Exception {
        return new StatusServer(configuration, getCuratorFramework(), "graphProperty", GraphPropertyRunner.class) {
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

    @Override
    public void process(Object messageId, JSONObject json) throws Exception {
        String propertyKey = json.optString("propertyKey", "");
        String propertyName = json.optString("propertyName", "");
        String workspaceId = json.optString("workspaceId", null);
        String visibilitySource = json.optString("visibilitySource", null);
        String priorityString = json.optString("priority", null);
        Priority priority = Priority.safeParse(priorityString);

        String graphVertexId = json.optString("graphVertexId");
        if (graphVertexId != null && graphVertexId.length() > 0) {
            Vertex vertex = graph.getVertex(graphVertexId, this.authorizations);
            if (vertex == null) {
                throw new VisalloException("Could not find vertex with id " + graphVertexId);
            }
            safeExecute(vertex, propertyKey, propertyName, workspaceId, visibilitySource, priority);
            return;
        }

        String graphEdgeId = json.optString("graphEdgeId");
        if (graphEdgeId != null && graphEdgeId.length() > 0) {
            Edge edge = graph.getEdge(graphEdgeId, this.authorizations);
            if (edge == null) {
                throw new VisalloException("Could not find edge with id " + graphEdgeId);
            }
            safeExecute(edge, propertyKey, propertyName, workspaceId, visibilitySource, priority);
            return;
        }

        throw new VisalloException("Could not find graphVertexId or graphEdgeId");
    }

    private void safeExecute(Element element, String propertyKey, String propertyName, String workspaceId, String visibilitySource, Priority priority) throws Exception {
        Property property;
        if ((propertyKey == null || propertyKey.length() == 0) && (propertyName == null || propertyName.length() == 0)) {
            property = null;
        } else {
            if (propertyKey == null) {
                property = element.getProperty(propertyName);
            } else {
                property = element.getProperty(propertyKey, propertyName);
            }
            if (property == null) {
                LOGGER.error("Could not find property [%s]:[%s] on vertex with id %s", propertyKey, propertyName, element.getId());
                return;
            }
        }
        safeExecute(element, property, workspaceId, visibilitySource, priority);
    }

    private void safeExecute(Element element, Property property, String workspaceId, String visibilitySource, Priority priority) throws Exception {
        String propertyText = property == null ? "[none]" : (property.getKey() + ":" + property.getName());

        List<GraphPropertyThreadedWrapper> interestedWorkerWrappers = findInterestedWorkers(element, property);
        if (interestedWorkerWrappers.size() == 0) {
            LOGGER.info("Could not find interested workers for element %s property %s", element.getId(), propertyText);
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            for (GraphPropertyThreadedWrapper interestedWorkerWrapper : interestedWorkerWrappers) {
                LOGGER.debug("interested worker for element %s property %s: %s", element.getId(), propertyText, interestedWorkerWrapper.getWorker().getClass().getName());
            }
        }

        GraphPropertyWorkData workData = new GraphPropertyWorkData(
                visibilityTranslator,
                element,
                property,
                workspaceId,
                visibilitySource,
                priority
        );

        LOGGER.debug("Begin work on element %s property %s", element.getId(), propertyText);
        if (property != null && property.getValue() instanceof StreamingPropertyValue) {
            StreamingPropertyValue spb = (StreamingPropertyValue) property.getValue();
            safeExecuteStreamingPropertyValue(interestedWorkerWrappers, workData, spb);
        } else {
            safeExecuteNonStreamingProperty(interestedWorkerWrappers, workData);
        }

        this.graph.flush();

        LOGGER.debug("Completed work on %s", propertyText);
    }

    private void safeExecuteNonStreamingProperty(List<GraphPropertyThreadedWrapper> interestedWorkerWrappers, GraphPropertyWorkData workData) throws Exception {
        for (GraphPropertyThreadedWrapper interestedWorkerWrapper1 : interestedWorkerWrappers) {
            interestedWorkerWrapper1.enqueueWork(null, workData);
        }
        for (GraphPropertyThreadedWrapper interestedWorkerWrapper : interestedWorkerWrappers) {
            interestedWorkerWrapper.dequeueResult(true);
        }
    }

    private void safeExecuteStreamingPropertyValue(List<GraphPropertyThreadedWrapper> interestedWorkerWrappers, GraphPropertyWorkData workData, StreamingPropertyValue streamingPropertyValue) throws Exception {
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

    private List<GraphPropertyThreadedWrapper> findInterestedWorkers(Element element, Property property) {
        Set<String> graphPropertyWorkerWhiteList = IterableUtils.toSet(VisalloProperties.GRAPH_PROPERTY_WORKER_WHITE_LIST.getPropertyValues(element));
        Set<String> graphPropertyWorkerBlackList = IterableUtils.toSet(VisalloProperties.GRAPH_PROPERTY_WORKER_BLACK_LIST.getPropertyValues(element));

        List<GraphPropertyThreadedWrapper> interestedWorkers = new ArrayList<>();
        for (GraphPropertyThreadedWrapper wrapper : workerWrappers) {
            String graphPropertyWorkerName = wrapper.getWorker().getClass().getName();
            if (graphPropertyWorkerWhiteList.size() > 0 && !graphPropertyWorkerWhiteList.contains(graphPropertyWorkerName)) {
                continue;
            }
            if (graphPropertyWorkerBlackList.contains(graphPropertyWorkerName)) {
                continue;
            }
            if (wrapper.getWorker().isHandled(element, property)) {
                interestedWorkers.add(wrapper);
            }
        }
        return interestedWorkers;
    }

    private String[] graphPropertyThreadedWrapperToNames(List<GraphPropertyThreadedWrapper> interestedWorkerWrappers) {
        String[] names = new String[interestedWorkerWrappers.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = interestedWorkerWrappers.get(i).getWorker().getClass().getName();
        }
        return names;
    }

    public void shutdown() {
        for (GraphPropertyThreadedWrapper wrapper : this.workerWrappers) {
            wrapper.stop();
        }
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

    @Override
    protected String getQueueName() {
        return workQueueNames.getGraphPropertyQueueName();
    }
}
