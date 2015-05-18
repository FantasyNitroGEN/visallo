package org.visallo.it;

import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.concurrent.Callable;

public class TimedExecution {
    private final TestClassAndMethod testClassAndMethod;
    private final VisalloLogger logger;

    public TimedExecution(TestClassAndMethod testClassAndMethod) {
        this.testClassAndMethod = testClassAndMethod;
        this.logger = VisalloLoggerFactory.getLogger(this.getClass());
    }

    public <T> Result<T> call(Callable<T> callable) throws Exception {
        long startTime = System.currentTimeMillis();
        T result = callable.call();
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        logger.info("%s#%s: %d ms", testClassAndMethod.getClassName(), testClassAndMethod.getMethodName(), elapsedTime);
        return new Result<>(elapsedTime, result);
    }

    public static class Result<T> {
        final long timeMillis;
        final T result;

        Result(long timeMillis, T result) {
            this.timeMillis = timeMillis;
            this.result = result;
        }
    }
}
