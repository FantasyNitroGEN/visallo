package org.visallo.core.ingest.graphProperty;

import org.visallo.core.model.workQueue.WorkQueueRepository;

public class GraphPropertyWorkerInitializer {
    private WorkQueueRepository workQueueRepository;

    public void setWorkQueueRepository(WorkQueueRepository workQueueRepository){
        this.workQueueRepository = workQueueRepository;
    }

    public void initialize(GraphPropertyWorker worker) {
        if(workQueueRepository != null) {
            worker.setWorkQueueRepository(workQueueRepository);
        }
    }
}
