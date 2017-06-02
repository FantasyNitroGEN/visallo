package org.visallo.core.model.ontology;

import com.google.common.collect.Lists;
import org.json.JSONException;
import org.vertexium.Authorizations;
import org.visallo.web.clientapi.model.ClientApiOntology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class Relationship implements OntologyElement, HasOntologyProperties {
    private final String parentIRI;
    private final List<String> domainConceptIRIs;
    private final List<String> rangeConceptIRIs;
    private final Collection<OntologyProperty> properties;

    protected Relationship(
            String parentIRI,
            List<String> domainConceptIRIs,
            List<String> rangeConceptIRIs,
            Collection<OntologyProperty> properties
    ) {
        this.parentIRI = parentIRI;
        this.domainConceptIRIs = domainConceptIRIs;
        this.rangeConceptIRIs = rangeConceptIRIs;
        this.properties = properties;
    }

    public abstract String getId();

    public abstract String getIRI();

    public String getParentIRI() {
        return parentIRI;
    }

    public abstract String getTitleFormula();

    public abstract String getSubtitleFormula();

    public abstract String getTimeFormula();

    public abstract String getDisplayName();

    public abstract Iterable<String> getInverseOfIRIs();

    public List<String> getDomainConceptIRIs() {
        return domainConceptIRIs;
    }

    public List<String> getRangeConceptIRIs() {
        return rangeConceptIRIs;
    }

    @Override
    public abstract boolean getUserVisible();

    @Override
    public abstract boolean getDeleteable();

    @Override
    public abstract boolean getUpdateable();

    public abstract String[] getIntents();

    public Collection<OntologyProperty> getProperties() {
        return properties;
    }

    public abstract void addIntent(String intent, Authorizations authorizations);

    public abstract void removeIntent(String intent, Authorizations authorizations);

    public void updateIntents(String[] newIntents, Authorizations authorizations) {
        ArrayList<String> toBeRemovedIntents = Lists.newArrayList(getIntents());
        for (String newIntent : newIntents) {
            if (toBeRemovedIntents.contains(newIntent)) {
                toBeRemovedIntents.remove(newIntent);
            } else {
                addIntent(newIntent, authorizations);
            }
        }
        for (String toBeRemovedIntent : toBeRemovedIntents) {
            removeIntent(toBeRemovedIntent, authorizations);
        }
    }

    public abstract void setProperty(String name, Object value, Authorizations authorizations);

    public abstract void removeProperty(String name, Authorizations authorizations);

    public ClientApiOntology.Relationship toClientApi() {
        try {
            ClientApiOntology.Relationship result = new ClientApiOntology.Relationship();
            result.setParentIri(getParentIRI());
            result.setTitle(getIRI());
            result.setDisplayName(getDisplayName());
            result.setDomainConceptIris(getDomainConceptIRIs());
            result.setRangeConceptIris(getRangeConceptIRIs());
            result.setUserVisible(getUserVisible());
            result.setDeleteable(getDeleteable());
            result.setUpdateable(getUpdateable());
            result.setTitleFormula(getTitleFormula());
            result.setSubtitleFormula(getSubtitleFormula());
            result.setTimeFormula(getTimeFormula());
            if (getIntents() != null) {
                result.getIntents().addAll(Arrays.asList(getIntents()));
            }

            Iterable<String> inverseOfIRIs = getInverseOfIRIs();
            for (String inverseOfIRI : inverseOfIRIs) {
                ClientApiOntology.Relationship.InverseOf inverseOf = new ClientApiOntology.Relationship.InverseOf();
                inverseOf.setIri(inverseOfIRI);
                inverseOf.setPrimaryIri(getPrimaryInverseOfIRI(getIRI(), inverseOfIRI));
                result.getInverseOfs().add(inverseOf);
            }

            if (this.properties != null) {
                for (OntologyProperty property : this.properties) {
                    result.getProperties().add(property.getTitle());
                }
            }

            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getPrimaryInverseOfIRI(String iri1, String iri2) {
        if (iri1.compareTo(iri2) > 0) {
            return iri2;
        }
        return iri1;
    }

    public static Collection<ClientApiOntology.Relationship> toClientApiRelationships(Iterable<Relationship> relationships) {
        Collection<ClientApiOntology.Relationship> results = new ArrayList<>();
        for (Relationship vertex : relationships) {
            results.add(vertex.toClientApi());
        }
        return results;
    }

    @Override
    public String toString() {
        return "Relationship{" +
                "iri='" + getIRI() + '\'' +
                '}';
    }
}
