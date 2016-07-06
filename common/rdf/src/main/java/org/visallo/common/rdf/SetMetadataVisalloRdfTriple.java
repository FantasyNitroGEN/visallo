package org.visallo.common.rdf;

import com.google.common.base.Strings;
import org.vertexium.ElementType;

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
        return String.format(
                "<%s> <%s%s> %s",
                getElementRdfString(),
                getPropertyRdfString(),
                getMetadataRdfString(),
                getValueRdfString()
        );
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
}
