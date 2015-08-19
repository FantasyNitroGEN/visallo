package org.visallo.core.ingest.graphProperty;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
import org.visallo.core.model.workQueue.WorkQueueRepository;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.vertexium.util.IterableUtils.toList;

public class GraphPropertyRunner extends WorkerBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GraphPropertyRunner.class);
    private Graph graph;
    private Authorizations authorizations;
    private List<GraphPropertyThreadedWrapper> workerWrappers = Lists.newArrayList();
    private User user;
    private UserRepository userRepository;
    private WorkQueueNames workQueueNames;
    private Configuration configuration;
    private VisibilityTranslator visibilityTranslator;
    private AtomicLong lastProcessedPropertyTime = new AtomicLong(0);

    @Override
    public void process(Object messageId, JSONObject json) throws Exception {
        GraphPropertyMessage message = new GraphPropertyMessage(json);
        if(!message.isValid()){
            throw new VisalloException(String.format("Cannot process unknown type of gpw message %s", json.toString()));
        }
        else if(message.canHandleByProperty()){
            safeExecuteHandlePropertyOnElement(message);
        }
        else{
            safeExecuteHandleEntireElement(message);
        }
    }

    public void prepare(User user) {
        prepare(user, new GraphPropertyWorkerInitializer());
    }

    public void prepare(User user, GraphPropertyWorkerInitializer repository){
        setUser(user);
        setAuthorizations(this.userRepository.getAuthorizations(user));
        prepareWorkers(repository);
    }

    public void prepareWorkers(GraphPropertyWorkerInitializer initializer) {
        FileSystem hdfsFileSystem = getFileSystem();

        List<TermMentionFilter> termMentionFilters = loadTermMentionFilters(hdfsFileSystem);

        GraphPropertyWorkerPrepareData workerPrepareData = new GraphPropertyWorkerPrepareData(
                configuration.toMap(),
                termMentionFilters,
                hdfsFileSystem,
                this.user,
                this.authorizations,
                InjectHelper.getInjector());
        Collection<GraphPropertyWorker> workers = InjectHelper.getInjectedServices(GraphPropertyWorker.class, configuration);
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

                if(initializer != null){
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

        if (failedToPrepareAtLeastOneGraphPropertyWorker) {
            throw new VisalloException("Failed to initialize at least one graph property worker. See the log for more details.");
        }
    }

    private FileSystem getFileSystem() {
        FileSystem hdfsFileSystem;
        org.apache.hadoop.conf.Configuration conf = configuration.toHadoopConfiguration();
        try {
            String hdfsRootDir = configuration.get(Configuration.HADOOP_URL, null);
            hdfsFileSystem = FileSystem.get(new URI(hdfsRootDir), conf, "hadoop");
        } catch (Exception e) {
            throw new VisalloException("Could not open hdfs filesystem", e);
        }
        return hdfsFileSystem;
    }

    public void addGraphPropertyThreadedWrappers(List<GraphPropertyThreadedWrapper> wrappers) {
        this.workerWrappers.addAll(wrappers);
    }

    public void addGraphPropertyThreadedWrappers(GraphPropertyThreadedWrapper... wrappers) {
        this.workerWrappers.addAll(Lists.newArrayList(wrappers));
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

    private void safeExecuteHandleEntireElement(GraphPropertyMessage message) throws Exception {
        Element element = getElement(message);
        for (Property property : element.getProperties()) {
            safeExecuteHandlePropertyOnElement(element, property, message);
        }
    }

    private Vertex getVertexFromMessage(GraphPropertyMessage message){
        String vertexId = message.getVertexId();
        Vertex vertex = graph.getVertex(vertexId, this.authorizations);
        ensureExists(vertex, "vertex", vertexId);
        return vertex;
    }

    private Edge getEdgeFromMessage(GraphPropertyMessage message){
        String edgeId = message.getEdgeId();
        Edge edge = graph.getEdge(edgeId, this.authorizations);
        ensureExists(edge, "edge", edgeId);
        return edge;
    }

    private void ensureExists(Element element, String type, String id){
        if (element == null) {
            throw new VisalloException(String.format("Could not find %s with id %s", type, id));
        }
    }

    private void safeExecuteHandlePropertyOnElement(GraphPropertyMessage message) throws Exception {
        Element element = getElement(message);
        Property property = null;
        if (StringUtils.isNotEmpty(message.getPropertyKey()) || StringUtils.isNotEmpty(message.getPropertyName())) {
           if (message.getPropertyKey() == null) {
                property = element.getProperty(message.getPropertyName());
            }
            else {
                property = element.getProperty(message.getPropertyKey(), message.getPropertyName());
            }

            if (property == null) {
                LOGGER.error("Could not find property [%s]:[%s] on vertex with id %s", message.getPropertyKey(), message.getPropertyName(), element.getId());
                return;
            }
        }

        safeExecuteHandlePropertyOnElement(element, property, message);
    }

    private void safeExecuteHandlePropertyOnElement(Element element, Property property, GraphPropertyMessage message) throws Exception {
        String propertyText = getPropertyText(property);

        List<GraphPropertyThreadedWrapper> interestedWorkerWrappers = findInterestedWorkers(element, property);
        if (interestedWorkerWrappers.size() == 0) {
            LOGGER.debug("Could not find interested workers for element %s property %s", element.getId(), propertyText);
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
                message.getWorkspaceId(),
                message.getVisibilitySource(),
                message.getPriority()
        );

        LOGGER.debug("Begin work on element %s property %s", element.getId(), propertyText);
        if (property != null && property.getValue() instanceof StreamingPropertyValue) {
            StreamingPropertyValue spb = (StreamingPropertyValue) property.getValue();
            safeExecuteStreamingPropertyValue(interestedWorkerWrappers, workData, spb);
        }
        else {
            safeExecuteNonStreamingProperty(interestedWorkerWrappers, workData);
        }

        lastProcessedPropertyTime.set(System.currentTimeMillis());

        this.graph.flush();

        LOGGER.debug("Completed work on %s", propertyText);
    }

    private String getPropertyText(Property property){
        return property == null ? "[none]" : (property.getKey() + ":" + property.getName());
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

    private Element getElement(GraphPropertyMessage message){
        if (message.canHandleVertex()) {
            return getVertexFromMessage(message);
        }
        else if(message.canHandleEdge()){
            return getEdgeFromMessage(message);
        }
        else {
            throw new VisalloException(String.format("Could not find %s or %s", GraphPropertyMessage.GRAPH_VERTEX_ID,  GraphPropertyMessage.GRAPH_EDGE_ID));
        }
    }

    public void shutdown() {
        for (GraphPropertyThreadedWrapper wrapper : this.workerWrappers) {
            wrapper.stop();
        }
    }

    public UserRepository getUserRepository(){
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

    public void setAuthorizations(Authorizations authorizations){
        this.authorizations = authorizations;
    }

    public long getLastProcessedTime(){
        return this.lastProcessedPropertyTime.get();
    }

    public void setUser(User user){
        this.user = user;
    }

    public User getUser(){
        return this.user;
    }

    @Override
    protected String getQueueName() {
        return workQueueNames.getGraphPropertyQueueName();
    }
}
