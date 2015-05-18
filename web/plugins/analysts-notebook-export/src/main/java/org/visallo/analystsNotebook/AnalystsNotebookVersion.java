package org.visallo.analystsNotebook;

import java.util.EnumSet;

public enum AnalystsNotebookVersion {
    VERSION_6("6",
              "http://visallo.org/analystsNotebook/v6",
              "General",
              EnumSet.of(AnalystsNotebookFeature.END_X,
                         AnalystsNotebookFeature.LINK_STYLE_STRENGTH)
    ),

    VERSION_7("7",
              "http://visallo.org/analystsNotebook/v7",
              "General",
              EnumSet.of(AnalystsNotebookFeature.SUMMARY,
                         AnalystsNotebookFeature.PRINT_SETTINGS,
                         AnalystsNotebookFeature.CHART_ITEM_X_POSITION)
    ),

    VERSION_8_5_1("8.5.1",
              "http://visallo.org/analystsNotebook/v8.5.1",
              "General",
              EnumSet.of(AnalystsNotebookFeature.SUMMARY,
                         AnalystsNotebookFeature.PRINT_SETTINGS,
                         AnalystsNotebookFeature.CHART_ITEM_X_POSITION)
    ),

    VERSION_8_9("8.9",
              "http://visallo.org/analystsNotebook/v8.9",
              "General",
              EnumSet.of(AnalystsNotebookFeature.SUMMARY,
                         AnalystsNotebookFeature.PRINT_SETTINGS,
                         AnalystsNotebookFeature.CHART_ITEM_X_POSITION,
                         AnalystsNotebookFeature.CUSTOM_IMAGE_COLLECTION,
                         AnalystsNotebookFeature.ICON_PICTURE)
    );

    private String string;
    private String ontologyConceptMetadataKeyPrefix;
    private String defaultIconFile;
    private EnumSet<AnalystsNotebookFeature> analystsNotebookFeatures;

    private AnalystsNotebookVersion(String string,
                                    String ontologyConceptMetadataKeyPrefix,
                                    String defaultIconFile,
                                    EnumSet<AnalystsNotebookFeature> analystsNotebookFeatures) {
        this.string = string;
        this.ontologyConceptMetadataKeyPrefix = ontologyConceptMetadataKeyPrefix;
        this.defaultIconFile = defaultIconFile;
        this.analystsNotebookFeatures = analystsNotebookFeatures;
    }

    @Override
    public String toString() {
        return string;
    }

    public String getOntologyConceptMetadataKeyPrefix() {
        return ontologyConceptMetadataKeyPrefix;
    }

    public String getDefaultIconFile() {
        return defaultIconFile;
    }

    public boolean supports(AnalystsNotebookFeature feature) {
        return analystsNotebookFeatures.contains(feature);
    }
}
