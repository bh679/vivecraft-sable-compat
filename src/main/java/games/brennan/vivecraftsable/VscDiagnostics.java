package games.brennan.vivecraftsable;

import com.mojang.logging.LogUtils;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/**
 * The tester's black box: everything someone needs to send back is a plain {@code latest.log}.
 *
 * <p>This mod fixes a bug that can only be reproduced in a VR headset, on a moving structure —
 * which the author cannot run. So the log has to answer, on its own, the questions a debugging
 * session would normally answer interactively:</p>
 *
 * <ol>
 *   <li><b>Did the fix even load?</b> {@code VscMixinPlugin} prints one line per mixin as it is
 *       applied. No lines ⇒ the mixin never applied and nothing else in the log means anything.</li>
 *   <li><b>What was installed?</b> One environment line with the versions that matter.</li>
 *   <li><b>Did the bug actually occur, and did we correct it?</b> Each teleport reports the raw
 *       destination Vivecraft chose, what it was corrected to, and how far apart those were. A
 *       large correction is the bug being caught; a zero correction on a train means the root
 *       cause is somewhere else and the theory behind this mod is wrong.</li>
 *   <li><b>Is melee still being repaired?</b> A periodic count, never a line per swing.</li>
 * </ol>
 *
 * <h2>Why it is safe to leave on</h2>
 *
 * <p>The costly thing here would be logging on a hot path. The melee hook runs on the render
 * thread many times a second, so it only ever increments a counter and emits at most one summary
 * line per {@value #MELEE_SUMMARY_INTERVAL_MS} ms. Teleports are user-initiated and rare, but are
 * still capped: full detail for the first {@value #TELEPORT_DETAIL_LIMIT}, then one line per
 * {@value #TELEPORT_SAMPLE_EVERY} so a marathon session cannot flood the file.</p>
 *
 * <p>Crucially, <b>no diagnostic makes an extra Sable call</b>. Whether the destination was on a
 * sub-level is inferred from whether Sable's projection moved it — information the fix already
 * has in hand — so measuring costs a vector comparison, not a lookup.</p>
 */
public final class VscDiagnostics {

    /** Grep handle. Everything this mod prints starts with it: {@code grep VSC latest.log}. */
    public static final String TAG = "[VSC]";

    /** Teleports logged in full before sampling kicks in. */
    static final int TELEPORT_DETAIL_LIMIT = 20;

    /** After the detail limit, log one in this many. */
    static final int TELEPORT_SAMPLE_EVERY = 25;

    /** Minimum gap between melee-clamp summary lines. */
    static final long MELEE_SUMMARY_INTERVAL_MS = 30_000L;

    /**
     * Correction distance (blocks) above which a teleport is called out as the bug being caught.
     * Sable parks sub-levels millions of blocks away, so a real catch is enormous; anything small
     * is ordinary float noise and should not be dressed up as a fix.
     */
    private static final double MEANINGFUL_CORRECTION = 1.0D;

    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile boolean environmentLogged;

    private static int teleports;
    private static int teleportsCorrected;

    private static long meleeClamps;
    private static long meleeClampsAtLastSummary;
    private static long lastMeleeSummaryMs;

    private VscDiagnostics() {}

    /**
     * Whether the {@code n}-th teleport (1-based) gets a log line.
     *
     * <p>Pure so the "cannot flood the log" promise is a test, not a claim: full detail while a
     * tester is doing the thing we asked them to do, then sampling so an all-day session stays
     * readable and cheap.</p>
     */
    public static boolean shouldLogTeleport(int n) {
        return n <= TELEPORT_DETAIL_LIMIT || n % TELEPORT_SAMPLE_EVERY == 0;
    }

    /**
     * Whether a melee summary is due. The melee hook runs on the render thread many times a
     * second, so this must be false almost always.
     *
     * @param nowMs      current time
     * @param lastMs     when a summary was last emitted ({@code 0} if never)
     */
    public static boolean shouldSummariseMelee(long nowMs, long lastMs) {
        return nowMs - lastMs >= MELEE_SUMMARY_INTERVAL_MS;
    }

    /**
     * One line naming the versions involved. Called at client setup, by which point {@link ModList}
     * is populated — a mod version is the first thing to check when a report does not add up.
     */
    public static void logEnvironment() {
        if (environmentLogged) {
            return;
        }
        environmentLogged = true;
        LOGGER.info("{} {} | vivecraft {} | sable {} | send logs/latest.log to report a VR issue",
            TAG, modVersion(VivecraftSableCompat.MOD_ID), modVersion("vivecraft"), modVersion("sable"));
    }

    private static String modVersion(String modId) {
        try {
            return ModList.get().getModContainerById(modId)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("ABSENT");
        } catch (RuntimeException e) {
            // Never let a diagnostic break the game. An unavailable ModList is not worth a crash.
            return "UNKNOWN";
        }
    }

    /**
     * Record one VR teleport.
     *
     * @param raw     the destination Vivecraft computed
     * @param used    the destination actually applied, after Sable's projection
     * @param landed  where the player ended up, read back after the move — proves it took effect
     */
    public static void teleport(Vec3 raw, Vec3 used, Vec3 landed) {
        teleports++;
        double correction = raw.distanceTo(used);
        boolean corrected = correction > MEANINGFUL_CORRECTION;
        if (corrected) {
            teleportsCorrected++;
        }

        if (!shouldLogTeleport(teleports)) {
            return;
        }

        // "ON-SUBLEVEL" is inferred, not queried: Sable's projection only moves a position that was
        // inside a sub-level, so a correction IS the detection. Free, and it cannot disagree with
        // what the fix actually did.
        LOGGER.info("{} teleport #{} {} correction={} raw={} used={} landed={}",
            TAG, teleports,
            corrected ? "ON-SUBLEVEL(corrected)" : "off-sublevel(unchanged)",
            fmt(correction), pos(raw), pos(used), pos(landed));

        if (corrected && landed.distanceTo(used) > MEANINGFUL_CORRECTION) {
            // The correction was computed but the player is not where we put them — something
            // downstream moved them again. Worth shouting about; this is the "still broken" shape.
            LOGGER.warn("{} teleport #{} LANDED OFF TARGET by {} — expected {}, got {}",
                TAG, teleports, fmt(landed.distanceTo(used)), pos(used), pos(landed));
        }
    }

    /** Record one oversized melee swing box being rebuilt. Hot path: counter only. */
    public static void meleeClamped() {
        meleeClamps++;
        long now = System.currentTimeMillis();
        if (!shouldSummariseMelee(now, lastMeleeSummaryMs)) {
            return;
        }
        lastMeleeSummaryMs = now;
        long since = meleeClamps - meleeClampsAtLastSummary;
        meleeClampsAtLastSummary = meleeClamps;
        LOGGER.info("{} melee: rebuilt {} oversized swing box(es) in the last {}s (total {})",
            TAG, since, MELEE_SUMMARY_INTERVAL_MS / 1000L, meleeClamps);
    }

    /** Final tally, written when the player leaves the world — the first thing to read in a report. */
    public static void logSummary() {
        if (teleports == 0 && meleeClamps == 0) {
            LOGGER.info("{} summary: no VR teleports and no melee clamps were recorded — either the "
                + "player never did either on a moving structure, or VR was not active", TAG);
            return;
        }
        LOGGER.info("{} summary: teleports={} (on-sublevel/corrected={}, off-sublevel={}) meleeClamps={}",
            TAG, teleports, teleportsCorrected, teleports - teleportsCorrected, meleeClamps);
    }

    private static String pos(Vec3 v) {
        return "(" + fmt(v.x) + "," + fmt(v.y) + "," + fmt(v.z) + ")";
    }

    private static String fmt(double d) {
        return String.format("%.2f", d);
    }
}
