package org.visallo.tools.ontology.ingest.codegen;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.tools.ontology.ingest.common.ConceptBuilder;
import org.visallo.tools.ontology.ingest.common.EntityBuilder;
import org.visallo.tools.ontology.ingest.common.PropertyAddition;
import org.visallo.tools.ontology.ingest.common.RelationshipBuilder;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.model.PropertyType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class EntityWriter {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(EntityWriter.class);
    private static final Class PROPERTY_ADDITION_CLASS = PropertyAddition.class;
    private static final Pattern IRI_FORMAT = Pattern.compile("^http://(.+)#(.+)$");
    private static final Pattern CLASSNAME_PART_MATCHER = Pattern.compile("^[a-zA-Z0-9]+$");

    private String outputDirectory;
    protected boolean writeCoreVisalloClasses;

    private Map<String, ClientApiOntology.Property> propertyMap;

    public EntityWriter(String outputDirectory, ClientApiOntology ontology, boolean writeCoreVisalloClasses) {
        this.outputDirectory = outputDirectory;
        propertyMap = ontology.getProperties().stream().collect(Collectors.toMap(ClientApiOntology.Property::getTitle, property -> property));
        this.writeCoreVisalloClasses = writeCoreVisalloClasses;
    }

    protected void writeClass(
            PrintWriter writer,
            String packageName,
            String className,
            String parentClass,
            String iri,
            List<ClientApiOntology.Property> properties,
            Consumer<PrintWriter> constructorProvider) {

        writer.println("package " + packageName + ";");
        writer.println();
        // TODO: Only included the required imports
        writer.println("import java.math.BigDecimal;");
        writer.println("import java.text.SimpleDateFormat;");
        writer.println("import java.util.Date;");
        writer.println("import java.util.Set;");
        writer.println("import java.util.HashSet;");
        writer.println("import org.vertexium.type.GeoPoint;");
        writer.println("import org.visallo.core.model.properties.types.*;");
        writer.println("import " + EntityBuilder.class.getName() + ";");
        writer.println("import " + ConceptBuilder.class.getName() + ";");
        writer.println("import " + RelationshipBuilder.class.getName() + ";");
        writer.println("import " + PROPERTY_ADDITION_CLASS.getName() + ";");
        writer.println();
        writer.println("public class " + className + " extends " + parentClass + " {");
        writer.println("  public static final String IRI = \"" + iri + "\";");
        writer.println();
        constructorProvider.accept(writer);
        writer.println();
        writer.println("  public String getIri() { return IRI; }");
        writePropertyMethods(writer, properties);
        writer.println('}');
    }

    protected void writePropertyMethods(PrintWriter writer, List<ClientApiOntology.Property> properties) {
        properties.forEach(property -> {
            String upperCamelCasePropertyName = classNameFromIri(property.getTitle());
            String constantName = constantNameFromClassName(upperCamelCasePropertyName);

            String propertyType = PropertyType.getTypeClass(property.getDataType()).getSimpleName();
            if (propertyType.equals("BigDecimal")) {
                propertyType = "Double";
            }

            String propertyAdditionType = PROPERTY_ADDITION_CLASS.getSimpleName() + "<" + propertyType + ">";
            String visalloPropertyType = (propertyType.equals("byte[]") ? "ByteArray" : propertyType) + "VisalloProperty";
            String helperMethodName = propertyType.equals("byte[]") ? "addByteArrayProperty" : "add" + propertyType + "Property";

            writer.println();
            writer.println("  public static final " + visalloPropertyType + " " + constantName + " = new " + visalloPropertyType + "(\"" + property.getTitle() + "\");");
            if (propertyType.equals("Date")) {
                writer.println(String.format(
                        "  public %s set%s(Object value, SimpleDateFormat dateFormat) { return add%s(\"\", value, dateFormat); }",
                        propertyAdditionType, upperCamelCasePropertyName, upperCamelCasePropertyName));
                writer.println(String.format(
                        "  public %s add%s(String key, Object value, SimpleDateFormat dateFormat) { return %s(%s.getPropertyName(), key, value, dateFormat); }",
                        propertyAdditionType, upperCamelCasePropertyName, helperMethodName, constantName
                ));
            } else {
                writer.println(String.format(
                        "  public %s set%s(Object value) { return add%s(\"\", value); }",
                        propertyAdditionType, upperCamelCasePropertyName, upperCamelCasePropertyName));
                writer.println(String.format(
                        "  public %s add%s(String key, Object value) { return %s(%s.getPropertyName(), key, value); }",
                        propertyAdditionType, upperCamelCasePropertyName, helperMethodName, constantName
                ));
            }

            LOGGER.debug("  %s property %s", propertyType, upperCamelCasePropertyName);
        });
    }

    protected String packageNameFromIri(String iri) {
        Matcher matcher = IRI_FORMAT.matcher(iri);
        if (matcher.matches()) {
            String[] baseIriParts = matcher.group(1).split("/", -1);

            String[] packageParts = baseIriParts[0].split("\\.", -1);
            ArrayUtils.reverse(packageParts);
            if (baseIriParts.length > 1) {
                packageParts = (String[]) ArrayUtils.addAll(packageParts, ArrayUtils.subarray(baseIriParts, 1, baseIriParts.length));
            }

            String packageName = String.join(".", packageParts);
            if (packageName.toLowerCase().equals("org.w3.www.2002.07.owl")) {
                packageName = "org.w3.www.owl";
            }
            return packageName;
        } else {
            LOGGER.error("Unsupported iri pattern %s", iri);
        }
        return null;
    }

    protected String constantNameFromClassName(String className) {
        String[] classNameParts = StringUtils.splitByCharacterTypeCamelCase(className);
        return Arrays.stream(classNameParts)
                .map(String::toUpperCase)
                .collect(Collectors.joining("_"));
    }

    protected String classNameFromIri(String iri) {
        Matcher matcher = IRI_FORMAT.matcher(iri);
        if (matcher.matches()) {
            String[] classNameParts = StringUtils.splitByCharacterTypeCamelCase(matcher.group(2));
            return Arrays.stream(classNameParts)
                    .filter(classNamePart -> CLASSNAME_PART_MATCHER.matcher(classNamePart).matches())
                    .map(StringUtils::capitalize)
                    .collect(Collectors.joining(""));
        } else {
            LOGGER.error("Unsupported iri pattern %s", iri);
        }
        return null;
    }

    protected PrintWriter createWriter(String packageName, String className) throws IOException {
        Path packagePath = Paths.get(outputDirectory, packageName.replaceAll("\\.", "/"));
        Files.createDirectories(packagePath);

        Path javaFileName = packagePath.resolve(className + ".java");
        return new PrintWriter(Files.newBufferedWriter(javaFileName, Charset.forName("UTF-8")));
    }

    protected List<ClientApiOntology.Property> findPropertiesByIri(List<String> propertyIris) {
        return propertyIris.stream().sorted()
                .map(propertyIri -> {
                    ClientApiOntology.Property property = propertyMap.get(propertyIri);
                    if (property == null) {
                        throw new VisalloException("Unable to locate property for iri: " + propertyIri);
                    }
                    return property;
                })
                .collect(Collectors.toList());
    }
}
