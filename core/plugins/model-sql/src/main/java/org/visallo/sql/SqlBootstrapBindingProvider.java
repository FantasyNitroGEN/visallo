package org.visallo.sql;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import org.visallo.core.bootstrap.BootstrapBindingProvider;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.sql.model.HibernateSessionManager;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;

import java.io.File;
import java.util.Set;

public class SqlBootstrapBindingProvider implements BootstrapBindingProvider {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(SqlBootstrapBindingProvider.class);
    private static final String HIBERNATE_CFG_XML = "hibernate.cfg.xml";
    private static final String HIBERNATE_PROPERTY_PREFIX = "hibernate";
    private static final Set<String> OTHER_HIBERNATE_PROPERTIES = ImmutableSet.of("show_sql", "hbm2ddl.auto");

    @Override
    public void addBindings(Binder binder, final Configuration visalloConfiguration) {
        binder.bind(HibernateSessionManager.class)
                .toProvider(new Provider<HibernateSessionManager>() {
                    @Override
                    public HibernateSessionManager get() {
                        org.hibernate.cfg.Configuration hibernateConfiguration = new org.hibernate.cfg.Configuration();

                        File configFile = visalloConfiguration.resolveFileName(HIBERNATE_CFG_XML);
                        if (!configFile.exists()) {
                            throw new VisalloException("Hibernate configuration file not found: " + HIBERNATE_CFG_XML);
                        }
                        hibernateConfiguration.configure(configFile);

                        for (String key : visalloConfiguration.getKeys()) {
                            if (key.startsWith(HIBERNATE_PROPERTY_PREFIX) || OTHER_HIBERNATE_PROPERTIES.contains(key)) {
                                String xmlValue = hibernateConfiguration.getProperty(key);
                                String visalloValue = visalloConfiguration.get(key, null);
                                if (visalloValue != null) {
                                    if (xmlValue == null) {
                                        LOGGER.info("setting Hibernate configuration %s with Visallo configuration value", key);
                                        hibernateConfiguration.setProperty(key, visalloValue);
                                    } else if (!visalloValue.equals(xmlValue)) {
                                        LOGGER.info("overriding Hibernate configuration %s with Visallo configuration value", key);
                                        hibernateConfiguration.setProperty(key, visalloValue);
                                    }
                                }
                            }
                        }

                        ServiceRegistry serviceRegistryBuilder = new StandardServiceRegistryBuilder().applySettings(hibernateConfiguration.getProperties()).build();
                        SessionFactory sessionFactory = hibernateConfiguration.buildSessionFactory(serviceRegistryBuilder);
                        return new HibernateSessionManager(sessionFactory);
                    }
                })
                .in(Scopes.SINGLETON);
    }
}
