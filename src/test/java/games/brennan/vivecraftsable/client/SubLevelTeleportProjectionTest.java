package games.brennan.vivecraftsable.client;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SubLevelTeleportProjection}.
 *
 * <p>The Sable projection is injected as a stub, so these run with no game, no VR and no
 * Sable on the classpath — the same approach {@link SwingAabbClampTest} takes.</p>
 */
class SubLevelTeleportProjectionTest {

    /** Stands in for Sable's behaviour off a sub-level: return the position untouched. */
    private static final SubLevelTeleportProjection.Projector IDENTITY = d -> d;

    @Test
    void zeroDestinationIsTreatedAsNoDestination() {
        assertTrue(SubLevelTeleportProjection.isNoDestination(Vec3.ZERO),
            "Vivecraft stores 'no valid teleport target' as all-zero, not null");
    }

    @Test
    void nullDestinationIsTreatedAsNoDestination() {
        assertTrue(SubLevelTeleportProjection.isNoDestination(null),
            "a null destination must not reach the projector");
    }

    @Test
    void realDestinationIsNotTreatedAsNoDestination() {
        assertFalse(SubLevelTeleportProjection.isNoDestination(new Vec3(12.5D, 64.0D, -30.0D)),
            "an ordinary destination must be projected, not skipped");
    }

    @Test
    void noDestinationSkipsTheProjectorEntirely() {
        AtomicInteger calls = new AtomicInteger();
        SubLevelTeleportProjection.Projector counting = d -> {
            calls.incrementAndGet();
            return d;
        };

        assertSame(Vec3.ZERO, SubLevelTeleportProjection.correct(Vec3.ZERO, counting),
            "the sentinel must be returned unchanged");
        assertEquals(0, calls.get(),
            "projecting the 'no target' sentinel is meaningless and must not happen");
    }

    @Test
    void offSubLevelDestinationPassesThroughUnchanged() {
        // Sable returns the input unchanged when the position is not inside a sub-level,
        // so ordinary teleports on solid ground must be a pure pass-through.
        Vec3 onTheGround = new Vec3(100.0D, 70.0D, 250.0D);

        assertSame(onTheGround, SubLevelTeleportProjection.correct(onTheGround, IDENTITY),
            "normal (non-sub-level) teleports must be untouched");
    }

    @Test
    void onSubLevelDestinationIsReplacedByTheProjectedPosition() {
        // The reported bug: the raycast destination is a position in the sub-level's distant
        // stored-block region, and Sable maps it back out to where the structure actually is.
        Vec3 storedRegionPos = new Vec3(20_480_000.0D, 64.0D, 20_480_000.0D);
        Vec3 whereTheTrainActuallyIs = new Vec3(-812.0D, 71.0D, 344.0D);

        Vec3 result = SubLevelTeleportProjection.correct(storedRegionPos, d -> whereTheTrainActuallyIs);

        assertSame(whereTheTrainActuallyIs, result,
            "a destination inside a sub-level must be projected back out to the real world");
    }

    @Test
    void nullFromProjectorFallsBackToTheOriginalDestination() {
        Vec3 destination = new Vec3(5.0D, 65.0D, 5.0D);

        assertSame(destination, SubLevelTeleportProjection.correct(destination, d -> null),
            "a projector returning null must not turn a teleport bug into a crash");
    }
}
