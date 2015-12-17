package org.visallo.model.directory;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import org.visallo.core.bootstrap.BootstrapBindingProvider;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;

public class LdapProviderBootstrapBindingProvider  implements BootstrapBindingProvider {
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
                            return new LdapSearchService(ldapServerConfiguration, ldapSearchConfiguration);
                        } catch (Exception e) {
                            throw new VisalloException("failed to configure ldap search service", e);
                        }
                    }
                })
                .in(Scopes.SINGLETON);
    }
}

