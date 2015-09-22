package org.visallo.core.action;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;

public class WorkQueueElementAction extends Action {
    public static final String PROPERTY_PRIORITY = "priority";
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public WorkQueueElementAction(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void execute(ActionExecuteParameters parameters, User user) {
        Priority priority = getPriority(parameters);
        this.workQueueRepository.pushElement(parameters.getElement(), priority);
    }

    private Priority getPriority(ActionExecuteParameters parameters) {
        String priorityStr = parameters.getData().optString(PROPERTY_PRIORITY);
        return Priority.safeParse(priorityStr);
    }

    public static JSONObject createWorkflowData(Priority priority) {
        JSONObject json = Action.createWorkflowData(WorkQueueElementAction.class);
        if (priority != null) {
            json.put(PROPERTY_PRIORITY, priority.name());
        }
        return json;
    }
}
