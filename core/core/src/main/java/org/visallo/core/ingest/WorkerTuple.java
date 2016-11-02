package org.visallo.core.ingest;

public class WorkerTuple {
    private final Object messageId;
    private final byte[] data;

    public WorkerTuple(Object messageId, byte[] data) {
        this.messageId = messageId;
        this.data = data;
    }

    public Object getMessageId() {
        return messageId;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "WorkerTuple{" +
                "messageId=" + messageId +
                '}';
    }
}
