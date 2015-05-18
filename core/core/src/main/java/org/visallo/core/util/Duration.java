package org.visallo.core.util;

import org.visallo.core.exception.VisalloException;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Duration {
    private static final Pattern BASIC_PATTERN = Pattern.compile("([-0-9\\.]+)[\\s*](.*)");
    private double time;
    private TimeUnit timeUnit;

    public Duration(double time, TimeUnit timeUnit) {
        this.time = time;
        this.timeUnit = timeUnit;
    }

    public Date addTo(Date date) {
        return new Date((long) (date.getTime() + getTimeInMillis()));
    }

    public double getTimeInMillis() {
        switch (timeUnit) {
            case NANOSECONDS:
                return time / 1000.0 / 1000.0;
            case MICROSECONDS:
                return time / 1000.0;
            case MILLISECONDS:
                return time;
            case SECONDS:
                return time * 1000.0;
            case MINUTES:
                return time * 1000.0 * 60.0;
            case HOURS:
                return time * 1000.0 * 60.0 * 60.0;
            case DAYS:
                return time * 1000.0 * 60.0 * 60.0 * 24.0;
        }
        throw new VisalloException("Unhandled time unit: " + timeUnit);
    }

    @Override
    public String toString() {
        return "Duration{" + time + " " + timeUnit + "}";
    }

    public static Duration parse(String str) {
        str = str.trim();
        if (str.equals("0")) {
            return new Duration(0, TimeUnit.MILLISECONDS);
        }

        Matcher m = BASIC_PATTERN.matcher(str);
        if (!m.matches()) {
            throw new VisalloException("Could not parse duration string: " + str);
        }
        try {
            double time = Double.parseDouble(m.group(1));
            TimeUnit timeUnit = parseTimeUnit(m.group(2));
            return new Duration(time, timeUnit);
        } catch (Exception ex) {
            throw new VisalloException("Could not parse duration string: " + str, ex);
        }
    }

    private static TimeUnit parseTimeUnit(String timeUnitString) {
        if (!timeUnitString.endsWith("s")) {
            timeUnitString = timeUnitString + "s";
        }
        timeUnitString = timeUnitString.toUpperCase();
        return TimeUnit.valueOf(timeUnitString);
    }
}
