package org.visallo.tools.ontology.ingest.codegen;

import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.tools.ontology.ingest.common.RelationshipBuilder;
import org.visallo.web.clientapi.model.ClientApiOntology;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class RelationshipWriter extends EntityWriter {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(RelationshipWriter.class);

    public RelationshipWriter(String outputDirectory, ClientApiOntology ontology, boolean writeCoreVisalloClasses) {
        super(outputDirectory, ontology, writeCoreVisalloClasses);
    }

    protected void writeClass(ClientApiOntology.Relationship relationship) {
        String relationshipPackage = packageNameFromIri(relationship.getTitle());
        if (relationshipPackage != null) {
            String relationshipClassName = classNameFromIri(relationship.getTitle());

            // Don't expose the visallo internal relationships to the generated code
            if (!writeCoreVisalloClasses && relationshipPackage.startsWith("org.visallo")) {
                return;
            }

            LOGGER.debug("Create relationship %s.%s", relationshipPackage, relationshipClassName);

            try (PrintWriter writer = createWriter(relationshipPackage, relationshipClassName)) {
                Consumer<PrintWriter> constructorWriter = methodWriter -> {
                    relationship.getDomainConceptIris().stream().sorted().forEach(outConceptIri -> {
                        String outVertexClassName = packageNameFromIri(outConceptIri) + "." + classNameFromIri(outConceptIri);
                        relationship.getRangeConceptIris().stream().sorted().forEach(inConceptIri -> {
                            String inVertexClassName = packageNameFromIri(inConceptIri) + "." + classNameFromIri(inConceptIri);
                            methodWriter.println();
                            methodWriter.println("  public " + relationshipClassName + "(String id, " + outVertexClassName + " outVertex, " + inVertexClassName + " inVertex) {");
                            methodWriter.println("    super(id, inVertex.getId(), inVertex.getIri(), outVertex.getId(), outVertex.getIri());");
                            methodWriter.println("  } ");
                        });
                    });
                };

                writeClass(
                        writer,
                        relationshipPackage,
                        relationshipClassName,
                        RelationshipBuilder.class.getName(),
                        relationship.getTitle(),
                        findPropertiesByIri(relationship.getProperties()),
                        constructorWriter);
            } catch (IOException e) {
                throw new VisalloException("Unable to create relationship class.", e);
            }
        }
    }
}
