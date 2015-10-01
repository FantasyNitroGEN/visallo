package org.visallo.vertexium.model.ontology;

import org.vertexium.Authorizations;
import org.vertexium.util.ConvertingIterable;
import org.visallo.core.model.ontology.OntologyProperties;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.Relationship;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class InMemoryRelationship extends Relationship {
    private String relationshipIRI;
    private String displayName;
    private List<Relationship> inverseOfs = new ArrayList<>();
    private List<String> intents = new ArrayList<>();
    private boolean userVisible;

    protected InMemoryRelationship(
            String parentIRI,
            String relationshipIRI,
            String displayName,
            List<String> domainConceptIRIs,
            List<String> rangeConceptIRIs,
            Collection<OntologyProperty> properties,
            String[] intents,
            boolean userVisible
    ) {
        super(parentIRI, domainConceptIRIs, rangeConceptIRIs, properties);
        this.relationshipIRI = relationshipIRI;
        this.displayName = displayName;
        this.intents.addAll(Arrays.asList(intents));
        this.userVisible = userVisible;
    }

    @Override
    public String getIRI() {
        return relationshipIRI;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Iterable<String> getInverseOfIRIs() {
        return new ConvertingIterable<Relationship, String>(inverseOfs) {
            @Override
            protected String convert(Relationship o) {
                return o.getIRI();
            }
        };
    }

    @Override
    public boolean getUserVisible() {
        return userVisible;
    }

    @Override
    public String[] getIntents() {
        return this.intents.toArray(new String[this.intents.size()]);
    }

    @Override
    public void addIntent(String intent, Authorizations authorizations) {
        this.intents.add(intent);
    }

    @Override
    public void removeIntent(String intent, Authorizations authorizations) {
        this.intents.remove(intent);
    }

    @Override
    public void setProperty(String name, Object value, Authorizations authorizations) {
        if (OntologyProperties.DISPLAY_NAME.getPropertyName().equals(name)) {
            this.displayName = (String) value;
        }
    }

    public void addInverseOf(Relationship inverseOfRelationship) {
        inverseOfs.add(inverseOfRelationship);
    }
}
