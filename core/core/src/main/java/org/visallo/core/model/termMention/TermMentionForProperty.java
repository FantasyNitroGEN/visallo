package org.visallo.core.model.termMention;

import org.visallo.core.model.properties.types.SingleValueVisalloProperty;

public class TermMentionForProperty extends SingleValueVisalloProperty<TermMentionFor, String> {
    public TermMentionForProperty(final String key) {
        super(key);
    }

    @Override
    public String wrap(TermMentionFor value) {
        return value.toString();
    }

    @Override
    public TermMentionFor unwrap(final Object value) {
        if (value == null) {
            return null;
        }
        return TermMentionFor.valueOf(value.toString());
    }
}

