package org.visallo.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.vertexium.TextIndexHint;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.OntologyProperties;
import org.visallo.web.clientapi.model.PropertyType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OWLOntologyUtil {
    private static final String OBJECT_PROPERTY_DOMAIN_IRI = "http://visallo.org#objectPropertyDomain";
    private static final String EXTENDED_DATA_TABLE_DOMAIN_IRI = "http://visallo.org#extendedDataTableDomain";

    public static String getLabel(OWLOntology o, OWLEntity owlEntity) {
        String bestLabel = owlEntity.getIRI().toString();
        for (OWLAnnotation annotation : EntitySearcher.getAnnotations(owlEntity, o)) {
            if (annotation.getProperty().isLabel()) {
                OWLLiteral value = (OWLLiteral) annotation.getValue();
                bestLabel = value.getLiteral();
                if (value.getLang().equals("") || value.getLang().equals("en")) {
                    return bestLabel;
                }
            }
        }
        return bestLabel;
    }

    public static String getAnnotationValueByUri(OWLOntology o, OWLEntity owlEntity, String uri) {
        for (OWLAnnotation annotation : EntitySearcher.getAnnotations(owlEntity, o)) {
            if (annotation.getProperty().getIRI().toString().equals(uri)) {
                OWLLiteral value = (OWLLiteral) annotation.getValue();
                return value.getLiteral();
            }
        }
        return null;
    }

    public static ImmutableList<String> getAnnotationValuesByUriOrNull(OWLOntology o, OWLEntity owlEntity, String uri) {
        List<String> values = new ArrayList<>();
        for (OWLAnnotation annotation : EntitySearcher.getAnnotations(owlEntity, o)) {
            if (annotation.getProperty().getIRI().toString().equals(uri)) {
                OWLLiteral value = (OWLLiteral) annotation.getValue();
                values.add(value.getLiteral());
            }
        }
        if (values.size() == 0) {
            return null;
        }
        return ImmutableList.copyOf(values);
    }

    public static Double getBoost(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, OntologyProperties.BOOST.getPropertyName());
        if (val == null) {
            return null;
        }
        return Double.parseDouble(val);
    }

    public static boolean getUserVisible(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, OntologyProperties.USER_VISIBLE.getPropertyName());
        return val == null || Boolean.parseBoolean(val);
    }

    public static boolean getSearchable(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, OntologyProperties.SEARCHABLE.getPropertyName());
        return val == null || Boolean.parseBoolean(val);
    }

    public static boolean getAddable(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, OntologyProperties.ADDABLE.getPropertyName());
        return val == null || Boolean.parseBoolean(val);
    }

    public static boolean getSortable(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, OntologyProperties.SORTABLE.getPropertyName());
        return val == null || Boolean.parseBoolean(val);
    }

    public static boolean getUpdateable(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, OntologyProperties.UPDATEABLE.getPropertyName());
        return val == null || Boolean.parseBoolean(val);
    }

    public static boolean getDeleteable(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, OntologyProperties.DELETEABLE.getPropertyName());
        return val == null || Boolean.parseBoolean(val);
    }

    public static String[] getIntents(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValuesByUri(o, owlEntity, OntologyProperties.INTENT.getPropertyName());
    }

    public static String[] getAnnotationValuesByUri(OWLOntology o, OWLEntity owlEntity, String uri) {
        List<String> results = new ArrayList<>();
        for (OWLAnnotation annotation : EntitySearcher.getAnnotations(owlEntity, o)) {
            if (annotation.getProperty().getIRI().toString().equals(uri)) {
                OWLLiteral value = (OWLLiteral) annotation.getValue();
                results.add(value.getLiteral());
            }
        }
        return results.toArray(new String[results.size()]);
    }

    public static Map<String, String> getPossibleValues(OWLOntology o, OWLEntity owlEntity) {
        String val = getAnnotationValueByUri(o, owlEntity, OntologyProperties.POSSIBLE_VALUES.getPropertyName());
        if (val == null || val.trim().length() == 0) {
            return null;
        }
        return JSONUtil.toStringMap(new JSONObject(val));
    }

    public static Collection<TextIndexHint> getTextIndexHints(OWLOntology o, OWLDataProperty owlEntity) {
        return TextIndexHint.parse(getAnnotationValueByUri(
                o,
                owlEntity,
                OntologyProperties.TEXT_INDEX_HINTS.getPropertyName()
        ));
    }

    public static Iterable<OWLAnnotation> getObjectPropertyDomains(OWLOntology o, OWLDataProperty owlDataTypeProperty) {
        List<OWLAnnotation> results = new ArrayList<>();
        for (OWLAnnotation annotation : EntitySearcher.getAnnotations(owlDataTypeProperty, o)) {
            if (annotation.getProperty().getIRI().toString().equals(OBJECT_PROPERTY_DOMAIN_IRI)) {
                results.add(annotation);
            }
        }
        return results;
    }

    public static Iterable<OWLAnnotation> getExtendedDataTableDomains(OWLOntology o, OWLDataProperty owlDataTypeProperty) {
        List<OWLAnnotation> results = new ArrayList<>();
        for (OWLAnnotation annotation : EntitySearcher.getAnnotations(owlDataTypeProperty, o)) {
            if (annotation.getProperty().getIRI().toString().equals(EXTENDED_DATA_TABLE_DOMAIN_IRI)) {
                results.add(annotation);
            }
        }
        return results;
    }

    public static String getDisplayType(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, OntologyProperties.DISPLAY_TYPE.getPropertyName());
    }

    public static String getPropertyGroup(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, OntologyProperties.PROPERTY_GROUP.getPropertyName());
    }

    public static String getValidationFormula(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, OntologyProperties.VALIDATION_FORMULA.getPropertyName());
    }

    public static String getDisplayFormula(OWLOntology o, OWLEntity owlEntity) {
        return getAnnotationValueByUri(o, owlEntity, OntologyProperties.DISPLAY_FORMULA.getPropertyName());
    }

    public static ImmutableList<String> getDependentPropertyIris(OWLOntology o, OWLEntity owlEntity) {
        List<String> results = new ArrayList<>();

        ImmutableList<String> dependentPropertyIris = getAnnotationValuesByUriOrNull(
                o,
                owlEntity,
                "http://visallo.org#dependentPropertyIris"
        );
        addAllDependentPropertyIris(results, dependentPropertyIris);

        ImmutableList<String> dependentPropertyIri = getAnnotationValuesByUriOrNull(
                o,
                owlEntity,
                OntologyProperties.EDGE_LABEL_DEPENDENT_PROPERTY
        );
        addAllDependentPropertyIris(results, dependentPropertyIri);

        if (results.size() == 0) {
            return null;
        }
        return ImmutableList.copyOf(results);
    }

    private static void addAllDependentPropertyIris(List<String> results, ImmutableList<String> dependentPropertyIris) {
        if (dependentPropertyIris == null) {
            return;
        }
        for (String dependentPropertyIri : dependentPropertyIris) {
            dependentPropertyIri = dependentPropertyIri.trim();
            if (dependentPropertyIri.startsWith("[")) {
                JSONArray array = new JSONArray(dependentPropertyIri);
                for (int i = 0; i < array.length(); i++) {
                    results.add(array.getString(i));
                }
            } else {
                results.add(dependentPropertyIri);
            }
        }
    }

    public static OWLOntology findOntology(List<OWLOntology> loadedOntologies, IRI documentIRI) {
        for (OWLOntology o : loadedOntologies) {
            Optional<IRI> oIRI = o.getOntologyID().getOntologyIRI();
            if (oIRI.isPresent() && documentIRI.equals(oIRI.get())) {
                return o;
            }
        }
        return null;
    }

    public static PropertyType getPropertyType(OWLOntology o, OWLDataProperty dataTypeProperty) {
        Collection<OWLDataRange> ranges = EntitySearcher.getRanges(dataTypeProperty, o);
        if (ranges.size() == 0) {
            return null;
        }
        if (ranges.size() > 1) {
            throw new VisalloException("Unexpected number of ranges on data property " + dataTypeProperty.getIRI().toString());
        }
        for (OWLDataRange range : ranges) {
            if (range instanceof OWLDatatype) {
                OWLDatatype datatype = (OWLDatatype) range;
                return getPropertyType(datatype.getIRI().toString());
            }
        }
        throw new VisalloException("Could not find range on data property " + dataTypeProperty.getIRI().toString());
    }

    public static PropertyType getPropertyType(String iri) {
        if ("http://www.w3.org/2001/XMLSchema#string".equals(iri)) {
            return PropertyType.STRING;
        }
        if ("http://www.w3.org/2001/XMLSchema#dateTime".equals(iri)) {
            return PropertyType.DATE;
        }
        if ("http://www.w3.org/2001/XMLSchema#date".equals(iri)) {
            return PropertyType.DATE;
        }
        if ("http://www.w3.org/2001/XMLSchema#gYear".equals(iri)) {
            return PropertyType.DATE;
        }
        if ("http://www.w3.org/2001/XMLSchema#gYearMonth".equals(iri)) {
            return PropertyType.DATE;
        }
        if ("http://www.w3.org/2001/XMLSchema#int".equals(iri)) {
            return PropertyType.DOUBLE;
        }
        if ("http://www.w3.org/2001/XMLSchema#double".equals(iri)) {
            return PropertyType.DOUBLE;
        }
        if ("http://www.w3.org/2001/XMLSchema#float".equals(iri)) {
            return PropertyType.DOUBLE;
        }
        if ("http://visallo.org#geolocation".equals(iri)) {
            return PropertyType.GEO_LOCATION;
        }
        if ("http://visallo.org#directory/entity".equals(iri)) {
            return PropertyType.DIRECTORY_ENTITY;
        }
        if ("http://visallo.org#currency".equals(iri)) {
            return PropertyType.CURRENCY;
        }
        if ("http://visallo.org#image".equals(iri)) {
            return PropertyType.IMAGE;
        }
        if ("http://visallo.org#extendedDataTable".equals(iri)) {
            return PropertyType.EXTENDED_DATA_TABLE;
        }
        if ("http://www.w3.org/2001/XMLSchema#hexBinary".equals(iri)) {
            return PropertyType.BINARY;
        }
        if ("http://www.w3.org/2001/XMLSchema#boolean".equals(iri)) {
            return PropertyType.BOOLEAN;
        }
        if ("http://www.w3.org/2001/XMLSchema#integer".equals(iri)) {
            return PropertyType.INTEGER;
        }
        if ("http://www.w3.org/2001/XMLSchema#nonNegativeInteger".equals(iri)) {
            return PropertyType.INTEGER;
        }
        if ("http://www.w3.org/2001/XMLSchema#positiveInteger".equals(iri)) {
            return PropertyType.INTEGER;
        }
        if ("http://www.w3.org/2001/XMLSchema#unsignedLong".equals(iri)) {
            return PropertyType.INTEGER;
        }
        if ("http://www.w3.org/2001/XMLSchema#unsignedByte".equals(iri)) {
            return PropertyType.INTEGER;
        }
        throw new VisalloException("Unhandled property type " + iri);
    }

    public static String getOWLAnnotationValueAsString(OWLAnnotation owlAnnotation) {
        return removeExtraQuotes(owlAnnotation.getValue().toString());
    }

    private static String removeExtraQuotes(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            str = str.substring(1, str.length() - 1);
        }
        return str;
    }
}
