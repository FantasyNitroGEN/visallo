package org.visallo.spark;

import org.apache.spark.api.java.JavaFutureAction;

import java.util.List;

public class NamedJavaFutureAction {
    public final String name;
    public final JavaFutureAction<Void> future;

    public NamedJavaFutureAction(String name, JavaFutureAction<Void> future) {
        this.name = name;
        this.future = future;
    }

    public static boolean isDone(List<NamedJavaFutureAction> futures) {
        for (NamedJavaFutureAction future : futures) {
            if (!future.future.isDone()) {
                return false;
            }
        }
        return true;
    }
}
