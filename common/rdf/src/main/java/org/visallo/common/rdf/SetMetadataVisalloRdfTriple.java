package org.visallo.common.rdf;

import com.google.common.base.Strings;
import org.vertexium.Authorizations;
import org.vertexium.Element;
import org.vertexium.ElementType;
import org.vertexium.mutation.ExistingElementMutation;

public class SetMetadataVisalloRdfTriple extends PropertyVisalloRdfTriple {
    private final String metadataName;
    private final String metadataVisibilitySource;

    public SetMetadataVisalloRdfTriple(
            ElementType elementType,
            String elementId,
            String elementVisibilitySource,
            String propertyKey,
            String propertyName,
            String propertyVisibilitySource,
            String metadataName,
            String metadataVisibilitySource,
            Object value
    ) {
        super(
                elementType,
                elementId,
                elementVisibilitySource,
                propertyKey,
                propertyName,
                propertyVisibilitySource,
                value
        );
        this.metadataName = metadataName;
        this.metadataVisibilitySource = metadataVisibilitySource;
    }

    public String getMetadataName() {
        return metadataName;
    }

    public String getMetadataVisibilitySource() {
        return metadataVisibilitySource;
    }

    @Override
    public String toString() {
        String warning = "\"Unhandled value type " + getValue().getClass().getName() + " to convert to RDF string\"";
        String value = getValueRdfString();
        return String.format("%s<%s> <%s%s> %s",
                value == null ? "# " : "",
                getElementRdfString(),
                getPropertyRdfString(),
                getMetadataRdfString(),
                value == null ? warning : value);
    }

    protected String getMetadataRdfString() {
        String result = String.format("@%s", escape(getMetadataName(), '@'));
        if (!Strings.isNullOrEmpty(getMetadataVisibilitySource())) {
            result += String.format("[%s]", getMetadataVisibilitySource());
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SetMetadataVisalloRdfTriple that = (SetMetadataVisalloRdfTriple) o;

        if (!metadataName.equals(that.metadataName)) {
            return false;
        }
        if (!metadataVisibilitySource.equals(that.metadataVisibilitySource)) {
            return false;
        }

        return super.equals(o);
    }

    @Override
    public ImportContext updateImportContext(
            ImportContext ctx,
            RdfTripleImportHelper rdfTripleImportHelper,
            Authorizations authorizations
    ) {
            Element element = ctx.save(authorizations);
            ExistingElementMutation<Element> m = element.prepareMutation();
            return new ImportContext(getElementId(), m);
    }
}
