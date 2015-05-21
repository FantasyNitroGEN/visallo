package org.visallo.core.util;

public class VisalloTime {
    private final String hours;
    private final String minutes;
    private final String seconds;
    private final String milliseconds;

    public VisalloTime(Integer hours, Integer minutes, Integer seconds, Integer milliseconds) {
        this(
                hours == null ? null : hours.toString(),
                minutes == null ? null : minutes.toString(),
                seconds == null ? null : seconds.toString(),
                milliseconds == null ? null : milliseconds.toString()
        );
    }

    public VisalloTime(String hours, String minutes, String seconds, String milliseconds) {
        this.hours = cleanHoursString(hours);
        this.minutes = cleanMinutesString(minutes);
        this.seconds = cleanSecondsString(seconds);
        this.milliseconds = cleanMillisecondsString(milliseconds);
    }

    private static String cleanMillisecondsString(String milliseconds) {
        milliseconds = milliseconds == null ? "???" : milliseconds;
        if (milliseconds.length() == 1) {
            if (milliseconds.charAt(0) == '?') {
                milliseconds = "?" + milliseconds;
            } else {
                milliseconds = "0" + milliseconds;
            }
        }
        if (milliseconds.length() == 2) {
            if (milliseconds.charAt(0) == '?') {
                milliseconds = "?" + milliseconds;
            } else {
                milliseconds = "0" + milliseconds;
            }
        }
        return milliseconds;
    }

    private static String cleanHoursString(String hours) {
        hours = hours == null ? "??" : hours;
        if (hours.length() == 1) {
            if (hours.charAt(0) == '?') {
                hours = "?" + hours;
            } else {
                hours = "0" + hours;
            }
        }
        return hours;
    }

    private static String cleanMinutesString(String minutes) {
        minutes = minutes == null ? "??" : minutes;
        if (minutes.length() == 1) {
            if (minutes.charAt(0) == '?') {
                minutes = "?" + minutes;
            } else {
                minutes = "0" + minutes;
            }
        }
        return minutes;
    }

    private static String cleanSecondsString(String seconds) {
        seconds = seconds == null ? "??" : seconds;
        if (seconds.length() == 1) {
            if (seconds.charAt(0) == '?') {
                seconds = "?" + seconds;
            } else {
                seconds = "0" + seconds;
            }
        }
        return seconds;
    }

    public String getHours() {
        return hours;
    }

    public int getHoursInt() {
        return Integer.parseInt(getHours());
    }

    public String getMinutes() {
        return minutes;
    }

    public int getMinutesInt() {
        return Integer.parseInt(getMinutes());
    }

    public String getSeconds() {
        return seconds;
    }

    public int getSecondsInt() {
        return Integer.parseInt(getSeconds());
    }

    public String getMilliseconds() {
        return milliseconds;
    }

    public int getMillisecondsInt() {
        return Integer.parseInt(getMilliseconds());
    }

    @Override
    public String toString() {
        return getHours() + ":" + getMinutes() + ":" + getSeconds() + "." + getMilliseconds();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VisalloTime that = (VisalloTime) o;

        if (!hours.equals(that.hours)) {
            return false;
        }
        if (!minutes.equals(that.minutes)) {
            return false;
        }
        if (!seconds.equals(that.seconds)) {
            return false;
        }
        return milliseconds.equals(that.milliseconds);

    }

    @Override
    public int hashCode() {
        int result = hours.hashCode();
        result = 31 * result + minutes.hashCode();
        result = 31 * result + seconds.hashCode();
        result = 31 * result + milliseconds.hashCode();
        return result;
    }
}
