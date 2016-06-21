package org.visallo.tools.ontology.ingest.common;

import org.vertexium.Visibility;
import org.visallo.core.user.User;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class IngestOptions {
    private Map<String, Object> defaultMetadata;
    private Long defaultTimestamp;
    private Visibility defaultVisibility;
    private User ingestUser;

    private boolean validateOntologyWhenSaving = true;

    public IngestOptions(User ingestUser) {
        this.setIngestUser(ingestUser);
    }

    public Map<String, Object> getDefaultMetadata() {
        return defaultMetadata;
    }

    public void setDefaultMetadata(Map<String, Object> defaultMetadata) {
        this.defaultMetadata = defaultMetadata;
    }

    public Long getDefaultTimestamp() {
        return defaultTimestamp;
    }

    public void setDefaultTimestamp(Long defaultTimestamp) {
        this.defaultTimestamp = defaultTimestamp;
    }

    public Visibility getDefaultVisibility() {
        return defaultVisibility;
    }

    public void setDefaultVisibility(Visibility defaultVisibility) {
        this.defaultVisibility = defaultVisibility;
    }

    public User getIngestUser() {
        return ingestUser;
    }

    public void setIngestUser(User ingestUser) {
        checkNotNull(ingestUser, "You must provide a valid ingest user.");
        this.ingestUser = ingestUser;
    }

    public boolean isValidateOntologyWhenSaving() {
        return validateOntologyWhenSaving;
    }

    public void setValidateOntologyWhenSaving(boolean validateOntologyWhenSaving) {
        this.validateOntologyWhenSaving = validateOntologyWhenSaving;
    }
}
