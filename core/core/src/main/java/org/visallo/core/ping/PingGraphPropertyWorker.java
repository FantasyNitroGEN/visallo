package org.visallo.core.ping;

import com.google.inject.Inject;
import org.vertexium.Element;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;

import java.io.InputStream;

@Name("Ping")
@Description("work on special Ping vertices to measure GPW wait time")
public class PingGraphPropertyWorker extends GraphPropertyWorker {
    @Inject
    public PingGraphPropertyWorker(
            AuthorizationRepository authorizationRepository,
            UserRepository userRepository
    ) {
        PingUtil.setup(authorizationRepository, userRepository);
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        Vertex vertex = (Vertex) data.getElement();
        PingUtil.gpwUpdate(vertex, getGraph(), getAuthorizations());
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        return element instanceof Vertex
                && PingOntology.IRI_CONCEPT_PING.equals(VisalloProperties.CONCEPT_TYPE.getPropertyValue(element))
                && property == null;
    }
}
