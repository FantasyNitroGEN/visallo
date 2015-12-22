package org.visallo.opennlpDictionary.model;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.v5analytics.simpleorm.SimpleOrmContext;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.visallo.core.user.User;

public class DictionaryEntryRepository {
    private static final String VISIBILITY_STRING = "";
    private final SimpleOrmSession simpleOrmSession;

    @Inject
    public DictionaryEntryRepository(SimpleOrmSession simpleOrmSession) {
        this.simpleOrmSession = simpleOrmSession;
    }

    public Iterable<DictionaryEntry> findAll(SimpleOrmContext simpleOrmContext) {
        return this.simpleOrmSession.findAll(DictionaryEntry.class, simpleOrmContext);
    }

    public Iterable<DictionaryEntry> findByConcept(final String concept, User user) {
        Iterable<DictionaryEntry> rows = findAll(user.getSimpleOrmContext());
        return Iterables.filter(rows, new Predicate<DictionaryEntry>() {
            @Override
            public boolean apply(DictionaryEntry dictionaryEntry) {
                return dictionaryEntry.getConcept().equals(concept);
            }
        });
    }

    public void delete(String id, User user) {
        this.simpleOrmSession.delete(DictionaryEntry.class, id, user.getSimpleOrmContext());
    }

    public DictionaryEntry createNew(String tokens, String concept) {
        return createNew(tokens, concept, null);
    }

    public DictionaryEntry createNew(String tokens, String concept, String resolvedName) {
        return new DictionaryEntry(
                tokens,
                concept,
                resolvedName
        );
    }

    public DictionaryEntry saveNew(String tokens, String concept, String resolvedName, User user) {
        DictionaryEntry entry = createNew(tokens, concept, resolvedName);
        this.simpleOrmSession.save(entry, VISIBILITY_STRING, user.getSimpleOrmContext());
        return entry;
    }

    public DictionaryEntry saveNew(String tokens, String concept, User user) {
        DictionaryEntry entry = createNew(tokens, concept);
        this.simpleOrmSession.save(entry, VISIBILITY_STRING, user.getSimpleOrmContext());
        return entry;
    }
}
