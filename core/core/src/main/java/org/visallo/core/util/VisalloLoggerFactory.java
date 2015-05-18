package org.visallo.core.util;

import com.google.common.base.Joiner;
import org.visallo.core.config.ConfigurationLoader;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class VisalloLoggerFactory {
    private static final Map<String, VisalloLogger> logMap = new HashMap<>();
    private static boolean initialized = false;
    private static boolean initializing = false;

    public static VisalloLogger getLogger(Class clazz, String processType) {
        if (processType != null) {
            setProcessType(processType);
        } else {
            setProcessType(clazz.getSimpleName());
        }
        ensureInitialized();
        return getLogger(clazz.getName());
    }

    public static VisalloLogger getLogger(Class clazz) {
        return getLogger(clazz, null);
    }

    private static void ensureInitialized() {
        synchronized (logMap) {
            if (!initialized && !initializing) {
                initializing = true;
                if (System.getProperty("logFileSuffix") == null) {
                    String hostname = null;
                    try {
                        hostname = InetAddress.getLocalHost().getHostName();
                    } catch (UnknownHostException e) {
                        System.err.println("Could not get host name: " + e.getMessage());
                    }
                    String logFileSuffix = "-" + Joiner.on("-").skipNulls().join(getProcessType(), hostname, ProcessUtil.getPid());
                    System.setProperty("logFileSuffix", logFileSuffix);
                }
                ConfigurationLoader.configureLog4j();
                initialized = true;
                initializing = false;
            }
        }
    }

    public static void setProcessType(String processType) {
        if (getProcessType() == null) {
            if (initializing) {
                System.err.println("setProcessType called too late");
            } else if (initialized) {
                getLogger(VisalloLoggerFactory.class).warn("setProcessType called too late");
            }
            System.setProperty("visallo.processType", processType);
        }
    }

    private static String getProcessType() {
        return System.getProperty("visallo.processType");
    }

    public static VisalloLogger getLogger(String name) {
        ensureInitialized();
        synchronized (logMap) {
            VisalloLogger visalloLogger = logMap.get(name);
            if (visalloLogger != null) {
                return visalloLogger;
            }
            visalloLogger = new VisalloLogger(LoggerFactory.getLogger(name));
            logMap.put(name, visalloLogger);
            return visalloLogger;
        }
    }
}
