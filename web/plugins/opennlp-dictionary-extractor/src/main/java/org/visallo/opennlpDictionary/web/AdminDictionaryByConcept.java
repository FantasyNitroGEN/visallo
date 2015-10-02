package org.visallo.opennlpDictionary.web;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import com.v5analytics.webster.utils.UrlUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.visallo.core.user.User;
import org.visallo.opennlpDictionary.model.DictionaryEntry;
import org.visallo.opennlpDictionary.model.DictionaryEntryRepository;

public class AdminDictionaryByConcept implements ParameterizedHandler {

    private DictionaryEntryRepository dictionaryEntryRepository;

    @Inject
    public AdminDictionaryByConcept(
            final DictionaryEntryRepository dictionaryEntryRepository) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
    }

    @Handle
    public JSONObject handle(
            @Required(name = "concept") String conceptParam,
            User user
    ) throws Exception {
        final String concept = UrlUtils.urlDecode(conceptParam);

        Iterable<DictionaryEntry> dictionary = dictionaryEntryRepository.findByConcept(concept, user);
        JSONArray entries = new JSONArray();
        JSONObject results = new JSONObject();
        for (DictionaryEntry entry : dictionary) {
            entries.put(entry.toJson());
        }

        results.put("entries", entries);

        return results;
    }
}
