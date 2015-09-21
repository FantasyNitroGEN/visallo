package org.visallo.core.time;

import java.util.Date;

public class MockTimeRepository extends TimeRepository {
    private Date now;

    public Date getNow() {
        return now;
    }

    public void setNow(Date now) {
        this.now = now;
    }
}
