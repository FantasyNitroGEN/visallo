package org.visallo.core.action;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.vertexium.Graph;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;

import java.util.Date;

public class SetPropertyToNowAction extends SetPropertyActionBase {
    @Inject
    public SetPropertyToNowAction(
            UserRepository userRepository,
            Graph graph,
            WorkQueueRepository workQueueRepository
    ) {
        super(userRepository, graph, workQueueRepository);
    }

    @Override
    protected Object getNewValue(ActionExecuteParameters parameters) {
        return new Date();
    }

    public static JSONObject createWorkflowData(String propertyKey, String propertyName, String visibility) {
        return SetPropertyActionBase.createWorkflowData(SetPropertyAction.class, propertyKey, propertyName, visibility);
    }
}
