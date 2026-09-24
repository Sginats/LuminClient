package com.lumin.luminclient.stats;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionAnalyticsTest {

    private static void resetStatsFile() {
        new File("build/tmp/luminclient-stats.json").delete();
    }

    @Test
    void recordAttemptUpdatesSummary() {
        resetStatsFile();
        SessionAnalytics analytics = new SessionAnalytics();
        analytics.recordAttempt(100.0, 50, true, 25.0, null);
        analytics.recordAttempt(120.0, 70, false, 0.0, "not-executed");

        SessionAnalytics.Summary summary = analytics.getSummary();
        assertEquals(2, summary.attempted);
        assertEquals(1, summary.succeeded);
        assertEquals(1, summary.failed);
        assertEquals(220.0, summary.totalSpend);
        assertEquals(25.0, summary.estimatedPnl);
        assertEquals(1, summary.failureReasons.get("not-executed"));
    }

    @Test
    void summaryResetsWhenDayChanges() throws Exception {
        resetStatsFile();
        SessionAnalytics analytics = new SessionAnalytics();
        analytics.recordAttempt(100.0, 50, true, 25.0, null);

        Field day = SessionAnalytics.class.getDeclaredField("day");
        day.setAccessible(true);
        day.set(analytics, "1900-01-01");

        SessionAnalytics.Summary summary = analytics.getSummary();
        assertEquals(0, summary.attempted);
        assertEquals(0, summary.succeeded);
        assertEquals(0, summary.failed);
    }

    @Test
    void summaryReloadsFromPersistedFile() {
        resetStatsFile();
        SessionAnalytics first = new SessionAnalytics();
        first.recordAttempt(300.0, 80, false, 0.0, "x");

        SessionAnalytics second = new SessionAnalytics();
        SessionAnalytics.Summary summary = second.getSummary();
        assertEquals(1, summary.attempted);
        assertEquals(0, summary.succeeded);
        assertEquals(1, summary.failed);
        assertEquals(300.0, summary.totalSpend);
    }
}
