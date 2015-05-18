package org.visallo.web.routes.notification;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.notification.SystemNotification;
import org.visallo.core.model.notification.SystemNotificationRepository;
import org.visallo.core.model.notification.SystemNotificationSeverity;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.BaseRequestHandler;
import com.v5analytics.webster.HandlerChain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SystemNotificationSave extends BaseRequestHandler {
    private final SystemNotificationRepository systemNotificationRepository;
    private final WorkQueueRepository workQueueRepository;
    private static final String ID_PARAMETER_NAME = "notificationId";
    private static final String SEVERITY_PARAMETER_NAME = "severity";
    private static final String TITLE_PARAMETER_NAME = "title";
    private static final String MESSAGE_PARAMETER_NAME = "message";
    private static final String START_DATE_PARAMETER_NAME = "startDate";
    private static final String EXTERNAL_URL_PARAMETER_NAME = "externalUrl";
    private static final String END_DATE_PARAMETER_NAME = "endDate";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm 'UTC'";

    @Inject
    public SystemNotificationSave(
            final SystemNotificationRepository systemNotificationRepository,
            final WorkQueueRepository workQueueRepository,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.systemNotificationRepository = systemNotificationRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String notificationId = getOptionalParameter(request, ID_PARAMETER_NAME);
        String severityParameter = getRequiredParameter(request, SEVERITY_PARAMETER_NAME);
        SystemNotificationSeverity severity = SystemNotificationSeverity.valueOf(severityParameter);
        String title = getRequiredParameter(request, TITLE_PARAMETER_NAME);
        String message = getRequiredParameter(request, MESSAGE_PARAMETER_NAME);
        String startDateParameter = getRequiredParameter(request, START_DATE_PARAMETER_NAME);
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date startDate = sdf.parse(startDateParameter);
        String endDateParameter = getOptionalParameter(request, END_DATE_PARAMETER_NAME);
        Date endDate = endDateParameter != null ? sdf.parse(endDateParameter) : null;
        String externalUrl = getOptionalParameter(request, EXTERNAL_URL_PARAMETER_NAME);
        User user = getUser(request);

        SystemNotification notification;

        if (notificationId == null) {
            notification = systemNotificationRepository.createNotification(severity, title, message, externalUrl, startDate, endDate, user);
        } else {
            notification = systemNotificationRepository.getNotification(notificationId, getUser(request));
            notification.setSeverity(severity);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setStartDate(startDate);
            notification.setEndDate(endDate);
            if (externalUrl != null) {
                notification.setExternalUrl(externalUrl);
            }
            notification = systemNotificationRepository.updateNotification(notification, user);
        }

        if (notification.isActive()) {
            workQueueRepository.pushSystemNotification(notification);
        } else {
            workQueueRepository.pushSystemNotificationUpdate(notification);
        }

        respondWithSuccessJson(response);
    }
}
