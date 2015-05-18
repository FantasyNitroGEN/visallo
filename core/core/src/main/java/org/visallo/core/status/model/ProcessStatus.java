package org.visallo.core.status.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProcessStatus extends Status {
    private String type;
    private Date startTime;
    private String hostname;
    private String osUser;
    private Map<String, String> env = new HashMap<>();
    private Jvm jvm = new Jvm();
    private Object configuration;

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getHostname() {
        return hostname;
    }

    public void setOsUser(String osUser) {
        this.osUser = osUser;
    }

    public String getOsUser() {
        return osUser;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public Jvm getJvm() {
        return jvm;
    }

    public void setConfiguration(Object configuration) {
        this.configuration = configuration;
    }

    public Object getConfiguration() {
        return configuration;
    }

    public static class Jvm {

        private String classpath;

        public void setClasspath(String classpath) {
            this.classpath = classpath;
        }

        public String getClasspath() {
            return classpath;
        }
    }
}
