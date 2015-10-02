package org.visallo.opennlpDictionary.web;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.json.JSONObject;
import org.visallo.core.user.User;
import org.visallo.opennlpDictionary.model.DictionaryEntry;
import org.visallo.opennlpDictionary.model.DictionaryEntryRepository;

public class AdminDictionaryEntryAdd implements ParameterizedHandler {

    private DictionaryEntryRepository dictionaryEntryRepository;

    @Inject
    public AdminDictionaryEntryAdd(
            final DictionaryEntryRepository dictionaryEntryRepository) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
    }

    @Handle
    public JSONObject handle(
            @Required(name = "tokens") String tokens,
            @Required(name = "concept") String concept,
            @Required(name = "resolvedName") String resolvedName,
            User user
    ) throws Exception {
        DictionaryEntry entry = dictionaryEntryRepository.saveNew(tokens, concept, resolvedName, user);

        JSONObject resultJson = new JSONObject();
        resultJson.put("success", true);
        resultJson.put("entry", entry.toJson());

        return resultJson;
    }
}
