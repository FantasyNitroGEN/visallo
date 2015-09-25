package org.visallo.core.action;

import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Visibility;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public abstract class SetPropertyActionBase extends Action {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(SetPropertyActionBase.class);
    public static final String PROPERTY_PROPERTY_KEY = "propertyKey";
    public static final String PROPERTY_PROPERTY_NAME = "propertyName";
    public static final String PROPERTY_VISIBILITY = "visibility";
    private final UserRepository userRepository;
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;

    protected SetPropertyActionBase(
            UserRepository userRepository,
            Graph graph,
            WorkQueueRepository workQueueRepository
    ) {
        this.userRepository = userRepository;
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void validateData(JSONObject actionData) {
        super.validateData(actionData);
        validateDataHas(actionData, PROPERTY_PROPERTY_KEY);
        validateDataHas(actionData, PROPERTY_PROPERTY_NAME);
        validateDataHas(actionData, PROPERTY_VISIBILITY);
    }

    @Override
    public void execute(ActionExecuteParameters parameters, User user) {
        String propertyKey = parameters.getData().getString(PROPERTY_PROPERTY_KEY);
        String propertyName = parameters.getData().getString(PROPERTY_PROPERTY_NAME);
        String visibility = parameters.getData().getString(PROPERTY_VISIBILITY);

        Object newValue = getNewValue(parameters);
        Authorizations authorizations = userRepository.getAuthorizations(user);
        Visibility vis = new Visibility(visibility);
        LOGGER.debug("setting property %s:%s[%s] = %s", propertyName, propertyKey, vis, newValue);
        parameters.getElement().addPropertyValue(propertyKey, propertyName, newValue, vis, authorizations);
        graph.flush();
        Property property = parameters.getElement().getProperty(propertyKey, propertyName);
        workQueueRepository.pushGraphPropertyQueue(parameters.getElement(), property, Priority.NORMAL);

    }

    protected abstract Object getNewValue(ActionExecuteParameters parameters);

    protected static JSONObject createActionData(Class clazz, String propertyKey, String propertyName, String visibility) {
        JSONObject json = Action.createActionData(clazz);
        json.put(PROPERTY_PROPERTY_KEY, propertyKey);
        json.put(PROPERTY_PROPERTY_NAME, propertyName);
        json.put(PROPERTY_VISIBILITY, visibility);
        return json;
    }
}
