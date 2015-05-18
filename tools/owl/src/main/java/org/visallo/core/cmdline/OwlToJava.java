package org.visallo.core.cmdline;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.visallo.core.cmdline.converters.IRIConverter;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.OntologyRepositoryBase;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.visallo.core.model.properties.types.*;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.vertexium.util.IterableUtils.toList;

@Parameters(commandDescription = "Create a Java class similar to VisalloProperties for a specific IRI")
public class OwlToJava extends CommandLineTool {
    @Parameter(names = {"--iri", "-i"}, required = true, arity = 1, converter = IRIConverter.class, description = "The IRI of the ontology you would like exported")
    private IRI iri;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new OwlToJava(), args);
    }

    @Override
    protected int run() throws Exception {
        SortedMap<String, String> sortedIntents = new TreeMap<>();
        SortedMap<String, String> sortedValues = new TreeMap<>();

        OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
        OWLOntologyManager m = getOntologyRepository().createOwlOntologyManager(config, null);

        OWLOntology o = m.getOntology(iri);
        if (o == null) {
            System.err.println("Could not find ontology " + iri);
            return 1;
        }

        System.out.println("public class Ontology {");
        System.out.println("    public static final String IRI = \"" + iri + "\";");
        System.out.println("");

        sortedValues.clear();
        for (OWLObjectProperty objectProperty : o.getObjectPropertiesInSignature()) {
            if (!o.isDeclared(objectProperty, Imports.EXCLUDED)) {
                continue;
            }
            exportObjectProperty(sortedValues, sortedIntents, o, objectProperty);
        }
        writeValues(sortedValues);
        System.out.println();

        sortedValues.clear();
        for (OWLClass owlClass : o.getClassesInSignature()) {
            if (!o.isDeclared(owlClass, Imports.EXCLUDED)) {
                continue;
            }
            exportClass(sortedValues, sortedIntents, o, owlClass);
        }
        writeValues(sortedValues);
        System.out.println();

        sortedValues.clear();
        for (OWLDataProperty dataProperty : o.getDataPropertiesInSignature()) {
            if (!o.isDeclared(dataProperty, Imports.EXCLUDED)) {
                continue;
            }
            exportDataProperty(sortedValues, sortedIntents, iri, o, dataProperty);
        }
        writeValues(sortedValues);
        System.out.println();

        writeValues(sortedIntents);
        System.out.println();

        System.out.println("}");

        return 0;
    }

    private void writeValues(SortedMap<String, String> sortedValues) {
        for (Map.Entry<String, String> sortedValue : sortedValues.entrySet()) {
            System.out.println(sortedValue.getValue());
        }
    }

    private void exportObjectProperty(SortedMap<String, String> sortedValues, SortedMap<String, String> sortedIntents, OWLOntology o, OWLObjectProperty objectProperty) {
        String iri = objectProperty.getIRI().toString();
        String label = OntologyRepositoryBase.getLabel(o, objectProperty);
        String javaConst = toJavaConst(label);

        addIntents(sortedIntents, OntologyRepositoryBase.getIntents(o, objectProperty));

        sortedValues.put(javaConst, String.format("    public static final String EDGE_LABEL_%s = \"%s\";", javaConst, iri));
    }

    private void exportClass(SortedMap<String, String> sortedValues, SortedMap<String, String> sortedIntents, OWLOntology o, OWLClass owlClass) {
        String iri = owlClass.getIRI().toString();
        String label = OntologyRepositoryBase.getLabel(o, owlClass);
        String javaConst = toJavaConst(label);

        addIntents(sortedIntents, OntologyRepositoryBase.getIntents(o, owlClass));

        sortedValues.put(javaConst, String.format("    public static final String CONCEPT_TYPE_%s = \"%s\";", javaConst, iri));
    }

    private void exportDataProperty(SortedMap<String, String> sortedValues, SortedMap<String, String> sortedIntents, IRI documentIri, OWLOntology o, OWLDataProperty dataProperty) {
        String iri = dataProperty.getIRI().toString();
        String label = OntologyRepositoryBase.getLabel(o, dataProperty);
        String javaConstName = toJavaConst(label);
        if (!iri.startsWith(documentIri.toString())) {
            String lastIriPart = getLastIriPart(iri);
            javaConstName = toJavaConst(lastIriPart) + "_" + javaConstName;
        }
        OWLDatatype range = (OWLDatatype) toList(EntitySearcher.getRanges(dataProperty, o)).get(0);
        String rangeIri = range.getIRI().toString();

        addIntents(sortedIntents, OntologyRepositoryBase.getIntents(o, dataProperty));

        String type;
        if ("http://www.w3.org/2001/XMLSchema#double".equals(rangeIri)) {
            type = DoubleVisalloProperty.class.getSimpleName();
        } else if ("http://www.w3.org/2001/XMLSchema#int".equals(rangeIri) || "http://www.w3.org/2001/XMLSchema#integer".equals(rangeIri)) {
            type = IntegerVisalloProperty.class.getSimpleName();
        } else if ("http://visallo.org#geolocation".equals(rangeIri)) {
            type = GeoPointVisalloProperty.class.getSimpleName();
        } else if ("http://www.w3.org/2001/XMLSchema#string".equals(rangeIri)) {
            type = StringVisalloProperty.class.getSimpleName();
        } else if ("http://www.w3.org/2001/XMLSchema#boolean".equals(rangeIri)) {
            type = BooleanVisalloProperty.class.getSimpleName();
        } else if ("http://visallo.org#currency".equals(rangeIri)) {
            type = DoubleVisalloProperty.class.getSimpleName(); // TODO should this be a CurrenyVisalloProperties
        } else if ("http://www.w3.org/2001/XMLSchema#dateTime".equals(rangeIri)) {
            type = DateVisalloProperty.class.getSimpleName();
        } else if ("http://www.w3.org/2001/XMLSchema#date".equals(rangeIri)) {
            type = DateVisalloProperty.class.getSimpleName();
        } else {
            throw new VisalloException("Could not map range type " + rangeIri);
        }

        sortedValues.put(javaConstName, String.format("    public static final %s %s = new %s(\"%s\");", type, javaConstName, type, iri));
    }

    private void addIntents(SortedMap<String, String> sortedIntents, String[] intents) {
        for (String intent : intents) {
            String javaConstName = toJavaConst(intent);
            sortedIntents.put(javaConstName, String.format("    public static final String INTENT_%s = \"%s\";", javaConstName, intent));
        }
    }

    private String getLastIriPart(String iri) {
        int lastSlash = iri.lastIndexOf('/');
        if (lastSlash < 0) {
            return iri;
        }
        String lastPart = iri.substring(lastSlash + 1);
        int hash = lastPart.indexOf('#');
        if (hash > 0) {
            lastPart = lastPart.substring(0, hash);
        }
        return lastPart;
    }

    private String toJavaConst(String label) {
        return label
                .toUpperCase()
                .replaceAll(" ", "_")
                .replaceAll("\\.", "_")
                .replaceAll("-", "_");
    }
}
