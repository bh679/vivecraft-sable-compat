package games.brennan.vivecraftsable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the log-volume promise: these diagnostics ship enabled, so "minimal and not performance
 * costing" has to be enforced somewhere rather than asserted in a comment.
 */
class VscDiagnosticsTest {

    @Test
    void everyEarlyTeleportIsLoggedInFull() {
        // A tester doing exactly what we asked — a handful of teleports — must produce a complete
        // record. Sampling those away would waste the trip.
        for (int n = 1; n <= VscDiagnostics.TELEPORT_DETAIL_LIMIT; n++) {
            assertTrue(VscDiagnostics.shouldLogTeleport(n), "teleport #" + n + " must be logged in full");
        }
    }

    @Test
    void laterTeleportsAreSampledNotDropped() {
        assertFalse(VscDiagnostics.shouldLogTeleport(VscDiagnostics.TELEPORT_DETAIL_LIMIT + 1),
            "past the detail limit, ordinary teleports stop being logged");
        int sampled = VscDiagnostics.TELEPORT_SAMPLE_EVERY * 10;
        assertTrue(VscDiagnostics.shouldLogTeleport(sampled),
            "sampling must keep a heartbeat so a long session still shows the fix working");
    }

    @Test
    void aLongSessionCannotFloodTheLog() {
        int logged = 0;
        for (int n = 1; n <= 10_000; n++) {
            if (VscDiagnostics.shouldLogTeleport(n)) {
                logged++;
            }
        }
        // 10k teleports is far beyond any real session; the log must stay a readable artifact.
        assertTrue(logged < 450, "10k teleports produced " + logged + " lines — too many to read");
        assertTrue(logged > 100, "10k teleports produced " + logged + " lines — too few to be useful");
    }

    @Test
    void meleeSummaryIsRateLimited() {
        long t0 = 1_000_000L;
        assertFalse(VscDiagnostics.shouldSummariseMelee(t0, t0),
            "a second call in the same millisecond must not log");
        assertFalse(VscDiagnostics.shouldSummariseMelee(t0 + VscDiagnostics.MELEE_SUMMARY_INTERVAL_MS - 1, t0),
            "just inside the interval must not log");
        assertTrue(VscDiagnostics.shouldSummariseMelee(t0 + VscDiagnostics.MELEE_SUMMARY_INTERVAL_MS, t0),
            "the interval boundary must log");
    }

    @Test
    void meleeAtFifteenHitsPerSecondLogsTwicePerMinute() {
        // The real shape of the bug: the swing clamp fires ~15x/sec on the render thread. Simulate a
        // minute of it and confirm the log gets a couple of lines, not nine hundred.
        long last = 0L;
        int lines = 0;
        for (long ms = 0; ms < 60_000L; ms += 1000L / 15L) {
            if (VscDiagnostics.shouldSummariseMelee(ms, last)) {
                last = ms;
                lines++;
            }
        }
        assertTrue(lines <= 3, "a minute of continuous clamping produced " + lines + " lines");
    }
}
