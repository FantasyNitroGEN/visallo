package org.visallo.web.util.js;

public class CachedCompilation {
    private String sourceMap;
    private String path;
    private String resourcePath;
    private String input;
    private String output;
    private Long lastModified;

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSourceMap() {
        return sourceMap;
    }

    public void setSourceMap(String sourceMap) {
        this.sourceMap = sourceMap;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isNecessary(long lastModified) {
        return getLastModified() == null ||
                getLastModified() != lastModified ||
                getOutput() == null;
    }
}