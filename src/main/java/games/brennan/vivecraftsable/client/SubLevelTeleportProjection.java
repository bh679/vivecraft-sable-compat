package games.brennan.vivecraftsable.client;

import net.minecraft.world.phys.Vec3;

/**
 * Decides the position a Vivecraft VR teleport should actually move the player to.
 *
 * <p>Kept OUT of the mixin package so it is a normal class (Mixin's transformer claims
 * everything under the mixin package) and unit-testable with no running game, no VR and
 * no Sable — the Sable call is injected as a {@link Projector}.</p>
 *
 * <p><b>The bug.</b> Sable keeps a sub-level's blocks in a far-away region of the level
 * and renders them elsewhere through the sub-level's pose. Vivecraft aims its teleport
 * with a block raycast, so on a moving carriage the destination it computes is a position
 * in that stored region, not the place the player is looking at. Vivecraft then applies it
 * with an <em>absolute</em> position write, which drops the player into empty space far
 * from the train — the reported "teleport puts me in the void".</p>
 *
 * <p><b>The fix.</b> Sable already defends its own vanilla teleport path by running the
 * destination through {@code SableCompanion.projectOutOfSubLevel}, which maps a position
 * inside a sub-level's stored region back out to where that sub-level actually is, and
 * returns the position <em>unchanged</em> when it is not inside one. Vivecraft's VR
 * teleport never passes through that guard, so this class applies the same one. Because
 * the projection is a no-op off a sub-level, it is safe to apply unconditionally: ordinary
 * teleports on solid ground are untouched.</p>
 *
 * <p>Fixing this on the client is sufficient for multiplayer too. Vivecraft sends the
 * server the player's position <em>after</em> the local move (see
 * {@code LocalPlayerVRMixin.vivecraft$directTeleport}, which reads {@code getX/Y/Z()}),
 * so correcting the local destination also corrects the coordinates the server is asked
 * to apply. No server-side counterpart is needed, and unmodified servers work.</p>
 */
public final class SubLevelTeleportProjection {

    private SubLevelTeleportProjection() {}

    /**
     * The Sable projection, as a seam. Implemented in the mixin as
     * {@code dest -> SableCompanion.INSTANCE.projectOutOfSubLevel(level, dest)}; replaced
     * by a stub in tests so the decision logic can be exercised without Sable loaded.
     */
    @FunctionalInterface
    public interface Projector {
        Vec3 project(Vec3 destination);
    }

    /**
     * True when {@code destination} is Vivecraft's "no valid target" value.
     *
     * <p>Vivecraft stores a missing destination as {@link Vec3#ZERO} rather than
     * {@code null} and guards its own teleport with the same all-zero test, so this must
     * be treated as "no destination", not as the world origin. Projecting it would be
     * meaningless, and on the vanishingly unlikely occasion that a sub-level's stored
     * region covers 0,0,0 it would also be actively wrong.</p>
     */
    public static boolean isNoDestination(Vec3 destination) {
        return destination == null
            || (destination.x == 0.0D && destination.y == 0.0D && destination.z == 0.0D);
    }

    /**
     * The position the teleport should use.
     *
     * @param destination the destination Vivecraft computed
     * @param projector   the Sable projection to apply
     * @return {@code destination} unchanged when there is no destination to fix, or when
     *         the projector declines to answer ({@code null}); otherwise the projected
     *         position. A projector that returns its input — which is what Sable does off
     *         a sub-level — makes this a pass-through, so normal play is unaffected.
     */
    public static Vec3 correct(Vec3 destination, Projector projector) {
        if (isNoDestination(destination)) {
            return destination;
        }
        Vec3 projected = projector.project(destination);
        // Defensive: a null here would turn a teleport bug into a crash. Sable's
        // implementation never returns null, but this class must not depend on that.
        return projected != null ? projected : destination;
    }
}
