package org.visallo.opennlpDictionary.web;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.user.User;
import org.visallo.opennlpDictionary.model.DictionaryEntryRepository;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;

public class AdminDictionaryEntryDelete implements ParameterizedHandler {

    private DictionaryEntryRepository dictionaryEntryRepository;

    @Inject
    public AdminDictionaryEntryDelete(final DictionaryEntryRepository dictionaryEntryRepository) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "entryRowKey") String strRowKey,
            User user
    ) throws Exception {
        dictionaryEntryRepository.delete(strRowKey, user);
        return VisalloResponse.SUCCESS;
    }
}
