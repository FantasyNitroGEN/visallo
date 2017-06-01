package org.visallo.vertexium.model.ontology;

import com.google.common.collect.ImmutableList;
import org.vertexium.Authorizations;
import org.visallo.core.model.ontology.ExtendedDataTableProperty;
import org.visallo.core.model.ontology.OntologyProperties;

import java.util.ArrayList;
import java.util.List;

public class InMemoryExtendedDataTableOntologyProperty extends InMemoryOntologyProperty implements ExtendedDataTableProperty {
    private String titleFormula;
    private String subtitleFormula;
    private String timeFormula;
    private List<String> tablePropertyIris = new ArrayList<>();

    @Override
    public String getTitleFormula() {
        return titleFormula;
    }

    public void setTitleFormula(String titleFormula) {
        this.titleFormula = titleFormula;
    }

    @Override
    public String getSubtitleFormula() {
        return subtitleFormula;
    }

    public void setSubtitleFormula(String subtitleFormula) {
        this.subtitleFormula = subtitleFormula;
    }

    @Override
    public String getTimeFormula() {
        return timeFormula;
    }

    @Override
    public ImmutableList<String> getTablePropertyIris() {
        return ImmutableList.copyOf(tablePropertyIris);
    }

    public void addTableProperty(String tablePropertyIri) {
        tablePropertyIris.add(tablePropertyIri);
    }

    public void setTimeFormula(String timeFormula) {
        this.timeFormula = timeFormula;
    }

    @Override
    public void setProperty(String name, Object value, Authorizations authorizations) {
        if (OntologyProperties.TITLE_FORMULA.getPropertyName().equals(name)) {
            this.titleFormula = (String) value;
        } else if (OntologyProperties.SUBTITLE_FORMULA.getPropertyName().equals(name)) {
            this.subtitleFormula = (String) value;
        } else if (OntologyProperties.TIME_FORMULA.getPropertyName().equals(name)) {
            this.timeFormula = (String) value;
        } else {
            super.setProperty(name, value, authorizations);
        }
    }
}
