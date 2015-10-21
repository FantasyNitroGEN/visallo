package org.visallo.groovy;

import com.google.inject.Inject;
import org.vertexium.Element;
import org.vertexium.Property;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.InputStream;

@Name("Groovy")
@Description("Runs groovy scripts as graph property workers")
public class GroovyGraphPropertyWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GroovyGraphPropertyWorker.class);
    private final GroovyGraphPropertyWorkerScriptRepository scriptRepository;

    @Inject
    public GroovyGraphPropertyWorker(GroovyGraphPropertyWorkerScriptRepository scriptRepository) {
        this.scriptRepository = scriptRepository;
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        for (ScriptData scriptData : scriptRepository.getScriptDatas()) {
            if (scriptData.isHandled(data.getElement(), data.getProperty())) {
                try {
                    scriptData.execute(in, data);
                } catch (Throwable ex) {
                    LOGGER.error("could not run execute method on script: %s", scriptData.toString(), ex);
                }
            }
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        scriptRepository.refreshScripts();
        for (ScriptData scriptData : scriptRepository.getScriptDatas()) {
            try {
                if (scriptData.isHandled(element, property)) {
                    return true;
                }
            } catch (Throwable ex) {
                LOGGER.error("could not run isHandle method on script: %s", scriptData.toString(), ex);
            }
        }
        return false;
    }
}
