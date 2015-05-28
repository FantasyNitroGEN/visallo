package org.visallo.core.util;

import org.visallo.core.exception.VisalloException;

import javax.xml.bind.DatatypeConverter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class VisalloDateTime {
    private static final String DATE_TIME_NO_TIME_ZONE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
    private final VisalloTime time;
    private final VisalloDate date;
    private final String timeZone;

    public VisalloDateTime(Integer year, Integer month, Integer date, Integer hour, Integer minutes, Integer seconds, Integer milliseconds, String timeZone) {
        this(
                new VisalloDate(year, month, date),
                new VisalloTime(hour, minutes, seconds, milliseconds),
                timeZone
        );
    }

    public VisalloDateTime(String year, String month, String date, String hour, String minutes, String seconds, String milliseconds, String timeZone) {
        this(new VisalloDate(year, month, date), new VisalloTime(hour, minutes, seconds, milliseconds), timeZone);
    }

    public VisalloDateTime(VisalloDate date, VisalloTime time, String timeZone) {
        this.date = date;
        this.time = time;
        this.timeZone = timeZone;
    }

    public Date toDateGMT() {
        return toDate(GMT);
    }

    public Date toDate(TimeZone destTimeZone) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
        cal.setTimeInMillis(0);
        cal.set(
                getDate().getYearInt(), getDate().getMonthInt() - 1, getDate().getDateInt(),
                getTime().getHoursInt(), getTime().getMinutesInt(), getTime().getSecondsInt()
        );
        cal.set(Calendar.MILLISECOND, getTime().getMillisecondsInt());

        Calendar destCal = Calendar.getInstance(destTimeZone);
        destCal.setTimeInMillis(cal.getTimeInMillis());
        return destCal.getTime();
    }

    public long getEpoch() {
        return toDateGMT().getTime();
    }

    public static VisalloDateTime create(Object obj) {
        return create(obj, TimeZone.getDefault());
    }

    public static VisalloDateTime create(Object obj, TimeZone defaultTimeZone) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return parse((String) obj, defaultTimeZone);
        }
        if (obj instanceof Date) {
            return create((Date) obj, defaultTimeZone);
        }
        if (obj instanceof Calendar) {
            return create(((Calendar) obj).getTime(), ((Calendar) obj).getTimeZone());
        }
        throw new VisalloException("Invalid object type to convert to " + VisalloDateTime.class.getSimpleName() + ": " + obj.getClass().getName());
    }

    public static VisalloDateTime create(Date date, TimeZone timeZone) {
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTime(date);
        return new VisalloDateTime(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DATE),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND),
                cal.get(Calendar.MILLISECOND),
                timeZone.getID()
        );
    }

    public static VisalloDateTime parse(String str, TimeZone defaultTimeZone) {
        return create(DatatypeConverter.parseDateTime(str), defaultTimeZone);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VisalloDateTime that = (VisalloDateTime) o;
        return this.getEpoch() == that.getEpoch();
    }

    @Override
    public int hashCode() {
        int result = time != null ? time.hashCode() : 0;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (timeZone != null ? timeZone.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return getDate().toString() + "T" + getTime().toString() + timeZoneToISO8601(getTimeZone());
    }

    private String timeZoneToISO8601(String timeZone) {
        if (timeZone.equals("GMT")) {
            return "Z";
        }
        TimeZone tz = TimeZone.getTimeZone(timeZone);
        int rawOffset = tz.getOffset(getEpoch());
        int totalMinutes = rawOffset / 1000 / 60;
        boolean negative = totalMinutes < 0;
        String negPosPrefix = negative ? "-" : "+";
        totalMinutes = Math.abs(totalMinutes);
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format("%s%02d:%02d", negPosPrefix, hours, minutes);
    }

    public VisalloTime getTime() {
        return time;
    }

    public VisalloDate getDate() {
        return date;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public Date getJavaDate() {
        return new Date(getEpoch());
    }
}
