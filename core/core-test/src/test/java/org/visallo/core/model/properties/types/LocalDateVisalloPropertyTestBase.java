package org.visallo.core.model.properties.types;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public abstract class LocalDateVisalloPropertyTestBase {
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    @Test
    public void wrapReturnsLegacyDateAtMidnightUTCForLocalDate() throws Exception {
        LocalDate localDate = LocalDate.now();

        Date legacyDateTime = createVisalloProperty().wrap(localDate);

        Calendar calendar = Calendar.getInstance(UTC_TIME_ZONE);
        calendar.setTime(legacyDateTime);
        assertThat(calendar.get(Calendar.YEAR), equalTo(localDate.getYear()));
        assertThat(calendar.get(Calendar.MONTH), equalTo(localDate.getMonthValue() - 1));
        assertThat(calendar.get(Calendar.DAY_OF_MONTH), equalTo(localDate.getDayOfMonth()));
        assertThat(calendar.get(Calendar.HOUR_OF_DAY), equalTo(0));
        assertThat(calendar.get(Calendar.MINUTE), equalTo(0));
        assertThat(calendar.get(Calendar.SECOND), equalTo(0));
        assertThat(calendar.get(Calendar.MILLISECOND), equalTo(0));
    }

    @Test
    public void unwrapReturnsLocalDateForLegacyDate() throws Exception {
        Date legacyDateTime = new Date();

        LocalDate localDate = createVisalloProperty().unwrap(legacyDateTime);

        Calendar calendar = Calendar.getInstance(UTC_TIME_ZONE);
        calendar.setTime(legacyDateTime);
        assertThat(localDate.getYear(), equalTo(calendar.get(Calendar.YEAR)));
        assertThat(localDate.getMonthValue(), equalTo(calendar.get(Calendar.MONTH) + 1));
        assertThat(localDate.getDayOfMonth(), equalTo(calendar.get(Calendar.DAY_OF_MONTH)));
    }

    protected abstract WrapsLocalDate createVisalloProperty();
}
