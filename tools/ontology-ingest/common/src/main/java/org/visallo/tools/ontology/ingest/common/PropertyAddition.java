package org.visallo.tools.ontology.ingest.common;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class PropertyAddition<T> {
    private String iri;
    private T value;
    private String key;
    private Map<String, Object> metadata;
    private Long timestamp;
    private String visibility;

    public PropertyAddition(String iri, String key, T value) {
        checkArgument(!Strings.isNullOrEmpty(iri), "IRI must be provided to create a PropertyAddition");
        checkNotNull(key, "Key must be provided to create a PropertyAddition");

        this.iri = iri;
        this.key = key;
        this.value = value;
    }

    public String getIri() {
        return iri;
    }

    public T getValue() {
        return value;
    }

    public String getKey() {
        return key;
    }

    public PropertyAddition<T> withMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    public PropertyAddition<T> withTimestamp(Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public PropertyAddition<T> withVisibility(String visibility) {
        this.visibility = visibility;
        return this;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getVisibility() {
        return visibility;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return iri.equals(((PropertyAddition<?>) o).iri) && key.equals(((PropertyAddition<?>) o).key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(iri, key);
    }
}
