package org.visallo.opennlpDictionary.web;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.json.JSONArray;
import org.json.JSONObject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.opennlpDictionary.model.DictionaryEntry;
import org.visallo.opennlpDictionary.model.DictionaryEntryRepository;

public class AdminDictionary implements ParameterizedHandler {
    private DictionaryEntryRepository dictionaryEntryRepository;

    @Inject
    public AdminDictionary(
            final DictionaryEntryRepository dictionaryEntryRepository,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
    }

    @Handle
    public JSONObject handle(
            User user
    ) throws Exception {

        Iterable<DictionaryEntry> dictionary = dictionaryEntryRepository.findAll(user.getSimpleOrmContext());
        JSONArray entries = new JSONArray();
        JSONObject results = new JSONObject();
        for (DictionaryEntry entry : dictionary) {
            entries.put(entry.toJson());
        }

        results.put("entries", entries);

        return results;
    }
}
