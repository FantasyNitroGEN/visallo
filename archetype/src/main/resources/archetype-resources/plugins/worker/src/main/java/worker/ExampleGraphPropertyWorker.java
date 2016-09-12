#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.worker;

import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.PropertyMetadata;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiWorkspace;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ${package}.worker.OntologyConstants.*;
import static org.visallo.core.model.properties.VisalloProperties.CONCEPT_TYPE;
import static org.visallo.core.model.properties.VisalloProperties.MIME_TYPE;
import static org.visallo.core.model.properties.VisalloProperties.RAW;

@Name("Example Visallo Graph Property Worker")
@Description("Creates person entities from an imported CSV file.")
public class ExampleGraphPropertyWorker extends GraphPropertyWorker {

    @Override
    public boolean isHandled(Element element, Property property) {
        // This worker is only interested in vertices with a RAW property whose content is a CSV file.
        return element instanceof Vertex &&
                property != null &&
                CONCEPT_TYPE.getProperty(element) == null &&
                RAW.getPropertyName().equals(property.getName()) &&
                "text/csv".equals(MIME_TYPE.getOnlyPropertyValue(element));
    }

    @Override
    public void execute(InputStream inputStream, GraphPropertyWorkData workData) throws Exception {
        // The visibility provided by workData will be used on new vertices, edges, and properties.
        // This visibilty originates from user input when uploading the CSV file.
        Visibility visibility = workData.getVisibility();
        VisibilityJson visibilityJson = workData.getVisibilityJson();

        // This is the vertex containing the CSV file content on its RAW property. RAW is a streaming property, which
        // is used for very large values. In this case, it's convenient to copy the value content to a temporary file.
        Vertex fileVertex = (Vertex) workData.getElement();
        StreamingPropertyValue raw = VisalloProperties.RAW.getPropertyValue(fileVertex);
        File file = copyToTempFile(raw);

        Set<Element> newElements = new HashSet<>();
        try {
            if (Contact.containsContacts(file)) {
                Authorizations authorizations = getAuthorizations();
                // Set the CONCEPT_TYPE property to identify the type of entity this vertex represents. This should
                // match an "owl:Class" defined in the ontology.
                CONCEPT_TYPE.setProperty(
                        fileVertex, CONTACTS_CSV_FILE_CONCEPT_TYPE, propertyMetadata(visibilityJson), visibility,
                        authorizations);

                Stream<Contact> contacts = Contact.readContacts(file);
                contacts.forEach(contact -> {
                    // Create a new vertex representing the person entity and create an edge representing the
                    // relationship between the CSV file and the person.
                    Vertex personVertex = createPersonVertex(contact, visibilityJson, visibility, authorizations);
                    Edge edge = createFileToPersonEdge(
                            fileVertex, personVertex, visibilityJson, visibility, authorizations);
                    newElements.add(personVertex);
                    newElements.add(edge);
                });
            }
        } finally {
            file.delete();
        }

        // Some graph implementations only persist changes on flush, so it should be called when element changes need
        // to be used immediately.
        getGraph().flush();

        // Get the workspace that the CSV file vertex was uploaded to.
        Workspace workspace = getWorkspaceRepository().findById(workData.getWorkspaceId(), getUser());

        // Add all new vertices to the workspace. Edges are automatically brought into the workspace.
        addVerticesToWorkspace(newElements, workspace);

        // Notify all browser clients so viewers of the workspace see the new elements immediately.
        notifyUserInterfaceClients(workspace);

        // Notify other workers that there are new elements.
        newElements.forEach(getWorkQueueRepository()::pushElement);
    }

    private void addVerticesToWorkspace(Set<Element> newElements, Workspace workspace) {
        Collection<WorkspaceRepository.Update> workspaceUpdates =
                newElements.stream()
                               .filter(element -> element instanceof Vertex)
                               .map(element -> new WorkspaceRepository.Update(element.getId(), true, null))
                               .collect(Collectors.toList());
        getWorkspaceRepository().updateEntitiesOnWorkspace(workspace, workspaceUpdates, getUser());
        getGraph().flush();
    }

    private void notifyUserInterfaceClients(Workspace workspace) {
        ClientApiWorkspace apiWorkspace =
                getWorkspaceRepository().toClientApi(workspace, getUser(), true, getAuthorizations());
        getWorkQueueRepository().pushWorkspaceChange(
                apiWorkspace, Collections.emptyList(), getUser().getUserId(), null);
    }

    private Vertex createPersonVertex(
            Contact contact, VisibilityJson visibilityJson, Visibility visibility, Authorizations authorizations) {
        VertexBuilder personBuilder = getGraph().prepareVertex("PERSON_" + UUID.randomUUID(), visibility);

        CONCEPT_TYPE.setProperty(
                personBuilder, PERSON_CONCEPT_TYPE, propertyMetadata(visibilityJson), visibility);

        PERSON_FULL_NAME_PROPERTY.setProperty(
                personBuilder, contact.name, propertyMetadata(visibilityJson), visibility);

        PERSON_PHONE_NUMBER_PROPERTY.setProperty(
                personBuilder, contact.phone, propertyMetadata(visibilityJson), visibility);

        PERSON_EMAIL_ADDRESS_PROPERTY.setProperty(
                personBuilder, contact.email, propertyMetadata(visibilityJson), visibility);

        setElementMetadata(personBuilder, visibilityJson, visibility);
        return personBuilder.save(authorizations);
    }

    private Edge createFileToPersonEdge(
            Vertex fileVertex, Vertex personVertex, VisibilityJson visibilityJson, Visibility visibility,
            Authorizations authorizations) {
        Edge edge = getGraph().addEdge(fileVertex, personVertex, HAS_ENTITY_EDGE_LABEL, visibility, authorizations);
        setElementMetadata(edge, visibilityJson, visibility, authorizations);
        return edge;
    }

    private void setElementMetadata(
            Element element, VisibilityJson visibilityJson, Visibility visibility, Authorizations authorizations) {
        VisalloProperties.VISIBILITY_JSON.setProperty(element, visibilityJson, visibility, authorizations);
        VisalloProperties.MODIFIED_BY.setProperty(element, getUser().getUserId(), visibility, authorizations);
        VisalloProperties.MODIFIED_DATE.setProperty(element, new Date(), visibility, authorizations);
    }

    private void setElementMetadata(ElementMutation element, VisibilityJson visibilityJson, Visibility visibility) {
        VisalloProperties.VISIBILITY_JSON.setProperty(element, visibilityJson, visibility);
        VisalloProperties.MODIFIED_BY.setProperty(element, getUser().getUserId(), visibility);
        VisalloProperties.MODIFIED_DATE.setProperty(element, new Date(), visibility);
    }

    private Metadata propertyMetadata(VisibilityJson visibilityJson) {
        Visibility defaultVisibility =  getVisibilityTranslator().getDefaultVisibility();
        return new PropertyMetadata(getUser(), visibilityJson, defaultVisibility).createMetadata();
    }

    private File copyToTempFile(StreamingPropertyValue spv) throws IOException {
        Path tempPath = Files.createTempFile(getClass().getName(), ".csv");
        try (InputStream inputStream = spv.getInputStream()) {
            Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempPath.toFile();
    }
}
