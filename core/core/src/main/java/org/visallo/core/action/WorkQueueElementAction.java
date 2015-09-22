package org.visallo.core.action;

import com.google.inject.Inject;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;

public class WorkQueueElementAction extends Action {
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
        String priorityStr = parameters.getData().optString("priority");
        return Priority.safeParse(priorityStr);
    }
}
