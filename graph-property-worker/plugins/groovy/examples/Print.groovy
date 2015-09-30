import org.vertexium.Element
import org.vertexium.Property
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData

public void execute(InputStream inputStream, GraphPropertyWorkData data) throws Exception {
    println "execute: " + data
}

public boolean isHandled(Element element, Property property) {
    println "isHandled: " + element + ", " + property
    return true;
}
