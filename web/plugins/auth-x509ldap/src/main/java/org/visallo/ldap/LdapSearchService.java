package org.visallo.ldap;

import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

public interface LdapSearchService {

    SearchResult searchPeople(Map<String, String> searchAttributes);

    SearchResult searchGroups(Map<String, String> searchAttributes);

    SearchResultEntry searchPeople(X509Certificate certificate);

    Set<String> searchGroups(SearchResultEntry personEntry);

    LdapSearchConfiguration getConfiguration();
}
