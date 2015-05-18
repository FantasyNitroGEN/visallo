package org.visallo.web;

import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.user.UserSessionCounterRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.UserStatus;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import java.io.Serializable;

public class SessionUser implements HttpSessionBindingListener, Serializable {
    private static final long serialVersionUID = -4886360466524045992L;
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(SessionUser.class);
    private String userId;

    public SessionUser(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public void valueBound(HttpSessionBindingEvent event) {
        // do nothing
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        try {
            UserSessionCounterRepository userSessionCounterRepository = InjectHelper.getInstance(UserSessionCounterRepository.class);
            WorkQueueRepository workQueueRepository = InjectHelper.getInstance(WorkQueueRepository.class);

            int sessionCount = userSessionCounterRepository.deleteSession(userId, event.getSession().getId());
            if (sessionCount < 1) {
                UserStatus status = UserStatus.OFFLINE;
                LOGGER.info("setting userId %s status to %s", userId, status);
                UserRepository userRepository = InjectHelper.getInstance(UserRepository.class);
                User user = userRepository.setStatus(userId, status);
                workQueueRepository.pushUserStatusChange(user, status);
            }
            workQueueRepository.pushSessionExpiration(userId, event.getSession().getId());
        } catch (Exception ex) {
            LOGGER.error("exception while unbinding user session for userId:%s", userId, ex);
        }
    }
}
