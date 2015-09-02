package org.visallo.core.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VisalloDateTimeTest {
    @Test
    public void testGetHumanTimeAgo() {
        assertEquals("0 ms ago", VisalloDateTime.getHumanTimeAgo(0));
        assertEquals("1 ms ago", VisalloDateTime.getHumanTimeAgo(1));
        assertEquals("999 ms ago", VisalloDateTime.getHumanTimeAgo(999));
        assertEquals("1 seconds ago", VisalloDateTime.getHumanTimeAgo(1000));
        assertEquals("1 minutes ago", VisalloDateTime.getHumanTimeAgo(60 * 1000));
        assertEquals("1 hours ago", VisalloDateTime.getHumanTimeAgo(60 * 60 * 1000));
        assertEquals("1 days ago", VisalloDateTime.getHumanTimeAgo(24 * 60 * 60 * 1000));
        assertEquals("5 days ago", VisalloDateTime.getHumanTimeAgo(5 * 24 * 60 * 60 * 1000));
    }
}