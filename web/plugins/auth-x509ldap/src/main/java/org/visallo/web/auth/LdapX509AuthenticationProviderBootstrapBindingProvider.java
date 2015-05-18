package org.visallo.web.auth;

import org.visallo.core.bootstrap.BootstrapBindingProvider;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.ldap.LdapSearchConfiguration;
import org.visallo.ldap.LdapSearchService;
import org.visallo.ldap.LdapSearchServiceImpl;
import org.visallo.ldap.LdapServerConfiguration;
import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Scopes;

public class LdapX509AuthenticationProviderBootstrapBindingProvider implements BootstrapBindingProvider {
    @Override
    public void addBindings(Binder binder, final Configuration configuration) {
        binder.bind(LdapSearchService.class)
                .toProvider(new Provider<LdapSearchService>() {
                    @Override
                    public LdapSearchService get() {
                        LdapServerConfiguration ldapServerConfiguration = new LdapServerConfiguration();
                        configuration.setConfigurables(ldapServerConfiguration, "ldap");

                        LdapSearchConfiguration ldapSearchConfiguration = new LdapSearchConfiguration();
                        configuration.setConfigurables(ldapSearchConfiguration, "ldap");

                        try {
                            return new LdapSearchServiceImpl(ldapServerConfiguration, ldapSearchConfiguration);
                        } catch (Exception e) {
                            throw new VisalloException("failed to configure ldap search service", e);
                        }
                    }
                })
                .in(Scopes.SINGLETON);
    }
}
