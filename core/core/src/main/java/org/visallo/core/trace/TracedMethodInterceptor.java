package org.visallo.core.trace;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Parameter;

public class TracedMethodInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (Trace.isEnabled()) {
            String description = invocation.getMethod().getDeclaringClass().getSimpleName() + "." + invocation.getMethod().getName();
            try (TraceSpan trace = Trace.start(description)) {
                Parameter[] methodParameters = invocation.getMethod().getParameters();
                Object[] invocationArguments = invocation.getArguments();
                int i;
                String argumentName = "";
                for (i = 0; i < methodParameters.length; i++) {
                    argumentName = methodParameters[i].getName();
                    addArgumentToData(trace, argumentName, invocationArguments[i]);
                }
                for (; i < invocationArguments.length; i++) {
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
