package org.visallo.core.action;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.vertexium.Graph;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;

public class SetPropertyAction extends SetPropertyActionBase {
    public static final String PROPERTY_VALUE = "value";

    @Inject
    public SetPropertyAction(
            UserRepository userRepository,
            Graph graph,
            WorkQueueRepository workQueueRepository
    ) {
        super(userRepository, graph, workQueueRepository);
    }

    @Override
    public void validateData(JSONObject actionData) {
        super.validateData(actionData);
        validateDataHas(actionData, PROPERTY_VALUE);
    }

    @Override
    protected Object getNewValue(ActionExecuteParameters parameters) {
        return parameters.getData().get(PROPERTY_VALUE);
    }

    public static JSONObject createWorkflowData(String propertyKey, String propertyName, Object value, String visibility) {
        JSONObject json = SetPropertyActionBase.createWorkflowData(SetPropertyAction.class, propertyKey, propertyName, visibility);
        json.put(PROPERTY_VALUE, value);
        return json;
    }
}
