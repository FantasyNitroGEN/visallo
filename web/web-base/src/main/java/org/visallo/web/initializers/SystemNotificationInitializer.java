package org.visallo.web.initializers;

import com.google.inject.Inject;
import org.visallo.core.model.notification.SystemNotificationService;

import javax.servlet.ServletContext;

public class SystemNotificationInitializer extends ApplicationBootstrapInitializer {
    private final SystemNotificationService systemNotificationService;

    @Inject
    public SystemNotificationInitializer(SystemNotificationService systemNotificationService) {
        this.systemNotificationService = systemNotificationService;
    }

    @Override
    public void initialize(ServletContext context) {
        this.systemNotificationService.start();
    }
}
