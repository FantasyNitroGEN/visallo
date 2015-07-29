package org.visallo.core.trace;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class TracedMethodInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (Trace.isEnabled()) {
            String description = invocation.getMethod().getDeclaringClass().getSimpleName() + "." + invocation.getMethod().getName();
            try (TraceSpan trace = Trace.start(description)) {
                Object[] invocationArguments = invocation.getArguments();
                for (int i = 0; i < invocationArguments.length; i++) {
                    String argumentName = "arg" + i;
                    addArgumentToData(trace, argumentName, invocationArguments[i]);
                }
                return invocation.proceed();
            }
        } else {
            return invocation.proceed();
        }
    }

    private void addArgumentToData(TraceSpan trace, String argumentName, Object invocationArgument) {
        String value;
        if (invocationArgument == null) {
            value = "(null)";
        } else {
            value = invocationArgument.toString();
        }
        trace.data(argumentName, value);
    }
}
