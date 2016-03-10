package org.visallo.core.action;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.vertexium.Graph;
import org.visallo.core.model.workQueue.WorkQueueRepository;

import java.util.Date;

public class SetPropertyToNowAction extends SetPropertyActionBase {
    @Inject
    public SetPropertyToNowAction(
            Graph graph,
            WorkQueueRepository workQueueRepository
    ) {
        super(graph, workQueueRepository);
    }

    @Override
    protected Object getNewValue(ActionExecuteParameters parameters) {
        return new Date();
    }

    public static JSONObject createActionData(String propertyKey, String propertyName, String visibility) {
        return SetPropertyActionBase.createActionData(SetPropertyToNowAction.class, propertyKey, propertyName, visibility);
    }
}
