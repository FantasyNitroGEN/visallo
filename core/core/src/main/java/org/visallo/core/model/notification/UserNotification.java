package org.visallo.core.model.notification;

import com.v5analytics.simpleorm.Entity;
import com.v5analytics.simpleorm.Field;
import org.json.JSONObject;

import java.util.*;

@Entity(tableName = "userNotifications")
public class UserNotification extends Notification {
    @Field
    private String userId;

    @Field
    private Date sentDate;

    @Field
    private Integer expirationAgeAmount;

    @Field
    private ExpirationAgeUnit expirationAgeUnit;

    @Field
    private boolean markedRead;

    // Used by SimpleOrm to create instance
    @SuppressWarnings("UnusedDeclaration")
    protected UserNotification() {
        super();
    }

    UserNotification(String userId, String title, String message, String actionEvent, JSONObject actionPayload, ExpirationAge expirationAge) {
        super(createRowKey(), title, message, actionEvent, actionPayload);
        this.userId = userId;
        this.sentDate = new Date();
        this.markedRead = false;
        if (expirationAge != null) {
            this.expirationAgeAmount = expirationAge.getAmount();
            this.expirationAgeUnit = expirationAge.getExpirationAgeUnit();
        } else {
            this.expirationAgeAmount = null;
            this.expirationAgeUnit = null;
        }
    }

    private static String createRowKey() {
        Date now = new Date();
        return Long.toString(now.getTime()) + ":" + UUID.randomUUID().toString();
    }

    public String getUserId() {
        return userId;
    }

    public Date getSentDate() {
        return sentDate;
    }

    public ExpirationAge getExpirationAge() {
        if (expirationAgeUnit != null && expirationAgeAmount != null) {
            return new ExpirationAge(expirationAgeAmount, expirationAgeUnit);
        }
        return null;
    }

    public boolean isMarkedRead() {
        return markedRead;
    }

    public void setMarkedRead(boolean markedRead) {
        this.markedRead = markedRead;
    }

    public boolean isActive() {
        if (isMarkedRead()) {
            return false;
        }
        Date now = new Date();
        Date expirationDate = getExpirationDate();
        Date sentDate = getSentDate();
        return sentDate.before(now) && (expirationDate == null || expirationDate.after(now));
    }

    public Date getExpirationDate() {
        ExpirationAge age = getExpirationAge();
        if (age == null) {
            return null;
        }

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTime(getSentDate());
        cal.add(age.getExpirationAgeUnit().getCalendarUnit(), age.getAmount());
        return cal.getTime();
    }

    @Override
    protected String getType() {
        return "user";
    }

    @Override
    public void populateJSONObject(JSONObject json) {
        json.put("userId", getUserId());
        json.put("sentDate", getSentDate());
        json.put("expirationAge", getExpirationAge());
        json.put("markedRead", isMarkedRead());
    }
}
