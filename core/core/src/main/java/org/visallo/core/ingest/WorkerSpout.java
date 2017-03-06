package org.visallo.core.ingest;

public abstract class WorkerSpout {
    public void open() {
    }

    public void close() {

    }

    public void ack(WorkerTuple workerTuple) {

    }

    /**
     * @deprecated  replaced by {@link #ack(WorkerTuple)} ()}
     */
    @Deprecated
    public void ack(Object msgId) {
        ack(new WorkerTuple(msgId, new byte[0]));
    }

    public void fail(WorkerTuple workerTuple) {

    }

    /**
     * @deprecated  replaced by {@link #fail(WorkerTuple)} ()}
     */
    @Deprecated
    public void fail(Object msgId) {
        fail(new WorkerTuple(msgId, new byte[0]));
    }

    /**
     * Get the next tuple from the queue. This method should poll and wait for a time period to
     * prevent spinning and causing high cpu load as the method calling this method will not sleep
     * if a null is returned. This method should also not wait indefinitely because the calling
     * method needs to perform periodic work.
     *
     * @return null, if no tuple is available in the polling period.
     */
    public abstract WorkerTuple nextTuple() throws Exception;
}
