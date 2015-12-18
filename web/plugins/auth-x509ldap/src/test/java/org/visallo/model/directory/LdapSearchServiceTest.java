package org.visallo.model.directory;

import com.google.common.collect.ImmutableMap;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.visallo.core.exception.VisalloException;

import java.util.Set;

import static org.junit.Assert.*;

public class LdapSearchServiceTest {
    private static InMemoryDirectoryServer ldapServer;
    private LdapSearchService service;

    @BeforeClass
    public static void setUp() throws Exception {
        ldapServer = LdapTestHelpers.configureInMemoryDirectoryServer();
        ldapServer.startListening();
    }

    @Before
    public void before() throws Exception {
        service = new LdapSearchService(LdapTestHelpers.getServerConfig(ldapServer), LdapTestHelpers.getSearchConfig());
    }

    @AfterClass
    public static void tearDown() {
        if (ldapServer != null) {
            ldapServer.shutDown(true);
        }
    }

    @Test
    public void searchForAliceWithCert() throws Exception {
        SearchResultEntry result = service.searchPeople(LdapTestHelpers.getPersonCertificate("alice"));

        assertNotNull(result);
        assertEquals("cn=Alice Smith,ou=people,dc=test,dc=visallo,dc=org", result.getDN());
        assertEquals("Alice Smith-Y-", result.getAttributeValue("displayName"));
        assertEquals("3", result.getAttributeValue("employeeNumber"));
        assertArrayEquals(LdapTestHelpers.getPersonCertificate("alice").getEncoded(), result.getAttributeValueBytes("userCertificate;binary"));

        Set<String> groups = service.searchGroups(result);
        assertNotNull(groups);
        assertEquals(2, groups.size());
        assertTrue(groups.contains("admins"));
        assertTrue(groups.contains("managers"));

        System.out.println("groups = " + groups);
        System.out.println(result.toLDIFString());
    }

    @Test
    public void searchForBobWithCert() throws Exception {
        SearchResultEntry result = service.searchPeople(LdapTestHelpers.getPersonCertificate("bob"));

        assertNotNull(result);
        assertEquals("cn=Bob Maluga,ou=people,dc=test,dc=visallo,dc=org", result.getDN());
        assertEquals("Bob Maluga-Y-", result.getAttributeValue("displayName"));
        assertEquals("4", result.getAttributeValue("employeeNumber"));

        Set<String> groups = service.searchGroups(result);
        assertNotNull(groups);
        assertEquals(1, groups.size());
        assertTrue(groups.contains("admins"));

        System.out.println("groups = " + groups);
        System.out.println(result.toLDIFString());
    }

    @Test
    public void searchForBobWithAttributes() {
        SearchResult result = service.searchPeople(ImmutableMap.of("cn", "Bob Maluga"));
        assertEquals(1, result.getEntryCount());
        SearchResultEntry entry = result.getSearchEntries().get(0);
        assertEquals("Bob Maluga", entry.getAttribute("cn").getValue());
    }

    @Test(expected = VisalloException.class)
    public void searchForNonExistentPersonWithCert() throws Exception {
        service.searchPeople(LdapTestHelpers.getPersonCertificate("diane"));
    }

    @Test
    public void searchForNonExistentPersonWithAttributes() {
        SearchResult result = service.searchPeople(ImmutableMap.of("cn", "Bob Marley"));
        assertEquals(0, result.getEntryCount());
    }

    @Test
    public void searchForAdminsGroupWithAttributes() throws Exception {
        service = new LdapSearchService(LdapTestHelpers.getServerConfig(ldapServer), LdapTestHelpers.getSearchConfigForGroups());
        SearchResult result = service.searchGroups(ImmutableMap.of("cn", "admins"));
        assertEquals(1, result.getEntryCount());
        SearchResultEntry entry = result.getSearchEntries().get(0);
        assertEquals("admins", entry.getAttribute("cn").getValue());
    }

    @Test
    public void searchForNonExistentGroupWithAttributes() throws Exception {
        service = new LdapSearchService(LdapTestHelpers.getServerConfig(ldapServer), LdapTestHelpers.getSearchConfigForGroups());
        SearchResult result = service.searchGroups(ImmutableMap.of("cn", "developers"));
        assertEquals(0, result.getEntryCount());
    }
}
