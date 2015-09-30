package org.visallo.groovy;

import groovy.lang.Script;
import org.vertexium.Element;
import org.vertexium.Property;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;

import java.io.File;
import java.io.InputStream;

class ScriptData {
    private final File file;
    private final long modifiedTime;
    private final Script script;

    ScriptData(File file, Script script) {
        this.file = file;
        this.modifiedTime = file.lastModified();
        this.script = script;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public Script getScript() {
        return script;
    }

    public boolean isHandled(Element element, Property property) {
        return (boolean) script.invokeMethod("isHandled", new Object[]{element, property});
    }

    public void execute(InputStream in, GraphPropertyWorkData data) {
        script.invokeMethod("execute", new Object[]{in, data});
    }

    @Override
    public String toString() {
        return file.getAbsolutePath();
    }
}
