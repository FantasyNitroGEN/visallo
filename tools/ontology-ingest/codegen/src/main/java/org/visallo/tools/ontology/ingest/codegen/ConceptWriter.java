package org.visallo.tools.ontology.ingest.codegen;

import com.google.common.base.Strings;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.tools.ontology.ingest.common.ConceptBuilder;
import org.visallo.web.clientapi.model.ClientApiOntology;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class ConceptWriter extends EntityWriter {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ConceptWriter.class);

    public ConceptWriter(String outputDirectory, ClientApiOntology ontology, boolean writeCoreVisalloClasses) {
        super(outputDirectory, ontology, writeCoreVisalloClasses);
    }

    protected void writeClass(ClientApiOntology.Concept concept) {
        String conceptPackage = packageNameFromIri(concept.getId());
        if (conceptPackage != null) {
            String conceptClassName = classNameFromIri(concept.getId());

            // Don't expose the visallo internal concepts to the generated code
            if (!writeCoreVisalloClasses && conceptPackage.startsWith("org.visallo") && !conceptClassName.equals("Root")) {
                return;
            }

            LOGGER.debug("Create concept %s.%s", conceptPackage, conceptClassName);

            try (PrintWriter writer = createWriter(conceptPackage, conceptClassName)) {
                String parentClass = ConceptBuilder.class.getSimpleName();
                if (!Strings.isNullOrEmpty(concept.getParentConcept())) {
                    parentClass = packageNameFromIri(concept.getParentConcept()) + "." + classNameFromIri(concept.getParentConcept());
                }

                Consumer<PrintWriter> constructorWriter = methodWriter -> writer.println("  public " + conceptClassName + "(String id) { super(id); }");

                writeClass(
                        writer,
                        conceptPackage,
                        conceptClassName,
                        parentClass,
                        concept.getId(),
                        findPropertiesByIri(concept.getProperties()),
                        constructorWriter);
            } catch (IOException e) {
                throw new VisalloException("Unable to create concept class.", e);
            }
        }
    }
}