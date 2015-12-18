package org.visallo.model.directory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import org.apache.commons.lang3.StringUtils;
import org.visallo.core.model.directory.DirectoryRepository;
import org.visallo.core.user.User;

import java.util.ArrayList;
import java.util.List;

public class LdapDirectoryRepository extends DirectoryRepository {
    private final LdapSearchService ldapSearchService;
    private final String peopleSearchAttribute;
    private final String groupsSearchAttribute;

    @Inject
    public LdapDirectoryRepository(LdapSearchService ldapSearchService) {
        this.ldapSearchService = ldapSearchService;

        LdapSearchConfiguration configuration = this.ldapSearchService.getConfiguration();
        List<String> userAttributes = configuration.getUserAttributes();
        if (userAttributes.isEmpty() || StringUtils.isBlank(userAttributes.get(0))) {
            throw new IllegalArgumentException("ldap configuration 'userAttributes' cannot be empty");
        }
        peopleSearchAttribute = userAttributes.get(0);
        groupsSearchAttribute = configuration.getGroupNameAttribute();
        if (StringUtils.isBlank(groupsSearchAttribute)) {
            throw new IllegalArgumentException("ldap configuration 'groupNameAttribute' cannot be empty");
        }
    }

    @Override
    public List<String> searchPeople(String search, User user) {
        search = String.format("*%s*", search);
        SearchResult peopleResult = ldapSearchService.searchPeople(ImmutableMap.of(peopleSearchAttribute, search));
        List<String> results = new ArrayList<>();
        for (SearchResultEntry searchResultEntry : peopleResult.getSearchEntries()) {
            results.add(searchResultEntry.getAttribute(peopleSearchAttribute).getValue());
        }
        return results;
    }

    @Override
    public List<String> searchGroups(String search, User user) {
        search = String.format("*%s*", search);
        SearchResult groupsResult = ldapSearchService.searchGroups(ImmutableMap.of(groupsSearchAttribute, search));
        List<String> results = new ArrayList<>();
        for (SearchResultEntry searchResultEntry : groupsResult.getSearchEntries()) {
            results.add(searchResultEntry.getAttribute(groupsSearchAttribute).getValue());
        }
        return results;
    }
}
