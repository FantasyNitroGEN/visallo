package org.visallo.core.model.ontology;

import com.google.common.collect.ImmutableList;

public interface ExtendedDataTableProperty {
    String getTitleFormula();

    String getSubtitleFormula();

    String getTimeFormula();

    ImmutableList<String> getTablePropertyIris();
}
