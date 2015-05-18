package org.visallo.core.model.notification;

import com.v5analytics.simpleorm.Entity;
import com.v5analytics.simpleorm.Field;
import org.json.JSONObject;

import java.util.Date;
import java.util.UUID;

@Entity(tableName = "systemNotifications")
public class SystemNotification extends Notification {
    @Field
    private SystemNotificationSeverity severity;

    @Field
    private Date startDate;

    @Field
    private Date endDate;

    // Used by SimpleOrm to create instance
    @SuppressWarnings("UnusedDeclaration")
    protected SystemNotification() {
        super();
    }

    public SystemNotification(Date startDate, String title, String message, String actionEvent, JSONObject actionPayload) {
        super(createId(startDate), title, message, actionEvent, actionPayload);
    }

    private static String createId(Date startDate) {
        return Long.toString(startDate.getTime()) + ":" + UUID.randomUUID().toString();
    }

    public SystemNotificationSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(SystemNotificationSeverity severity) {
        this.severity = severity;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        if (startDate == null) {
            startDate = new Date();
        }
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setExternalUrl(String externalUrl) {
        this.setActionEvent(ACTION_EVENT_EXTERNAL_URL);
        JSONObject payload = new JSONObject();
        payload.put("url", externalUrl);
        this.setActionPayload(payload);
    }

    public boolean isActive() {
        Date now = new Date();
        Date endDate = getEndDate();
        return getStartDate().before(now) && (endDate == null || endDate.after(now));
    }

    @Override
    protected String getType() {
        return "system";
    }

    @Override
    public void populateJSONObject(JSONObject json) {
        json.put("severity", getSeverity());
        Date startDate = getStartDate();
        if (startDate != null) {
            json.put("startDate", startDate.getTime());
        }
        Date endDate = getEndDate();
        if (endDate != null) {
            json.put("endDate", endDate.getTime());
        }
    }
}
