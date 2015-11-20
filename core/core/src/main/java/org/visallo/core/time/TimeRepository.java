package org.visallo.core.time;

import java.util.Date;

public class TimeRepository {
    public Date getNow() {
        return new Date();
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
