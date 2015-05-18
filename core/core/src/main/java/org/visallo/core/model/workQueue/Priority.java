package org.visallo.core.model.workQueue;

public enum Priority {
    LOW,
    NORMAL,
    HIGH;

    public static Priority safeParse(String priorityString) {
        try {
            if (priorityString == null || priorityString.length() == 0) {
                return Priority.NORMAL;
            }
            return Priority.valueOf(priorityString);
        } catch (Exception ex) {
            return Priority.NORMAL;
        }
    }
}
