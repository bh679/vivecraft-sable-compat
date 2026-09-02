package games.brennan.vivecraftsable.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.companion.SableCompanion;
import games.brennan.vivecraftsable.client.SubLevelTeleportProjection;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops Vivecraft's VR teleport dropping the player into the void when they teleport while
 * standing on a Sable sub-level (e.g. a moving train carriage).
 *
 * <p><b>The bug.</b> Sable stores a sub-level's blocks in a distant region of the level and
 * renders them elsewhere via the sub-level's pose. Vivecraft picks its teleport destination
 * with a block raycast, so on a moving structure the destination is a position in that
 * stored region rather than the spot the player is looking at. {@code activeProcess} then
 * applies it with {@code LocalPlayer.moveTo} — an absolute position write — and the player
 * lands far from the train in empty space.</p>
 *
 * <p><b>Why Sable's own guard misses it.</b> Sable already wraps the vanilla teleport path
 * ({@code ServerPlayer.teleportTo}) and rewrites the destination through
 * {@code projectOutOfSubLevel}. Vivecraft does not use {@code teleportTo}, so it slips past
 * that guard entirely. This mixin applies the same guard at Vivecraft's own call.</p>
 *
 * <p><b>Client-side is enough.</b> Vivecraft reports the teleport to the server as the
 * player's position <em>after</em> this call ({@code LocalPlayerVRMixin} sends
 * {@code getX/Y/Z()}), so fixing the local move fixes the coordinates the server applies
 * too. This works against unmodified servers.</p>
 *
 * <p><b>Mixin targets</b> (verified with {@code javap} against
 * {@code vivecraft-1.21.1-1.3.15-neoforge.jar}; Vivecraft is not a compile dependency, so
 * the class is targeted by string). {@code LocalPlayer.moveTo(DDD)V} occurs exactly once in
 * {@code activeProcess}, so the wrap is unambiguous and needs no ordinal. Note the method is
 * {@code moveTo} in 1.21.1 — Mojang renamed it to {@code snapTo} in a later version, which
 * is what Vivecraft's current sources call it; do not copy the newer name back here.</p>
 *
 * <p>Deliberately narrow: only the teleport's final position write is touched. The aiming
 * arc, teleport limits, energy cost and sounds are all left to Vivecraft.</p>
 *
 * <p><b>Why the {@link Position} cast.</b> {@code projectOutOfSubLevel} is overloaded, and the
 * {@code (Level, Vec3)} overload Java would otherwise pick as the more specific match is
 * {@code @Deprecated} + {@code @ScheduledForRemoval(inVersion = "2.0.0")} — Sable is already
 * past 2.0.0, so it can disappear in any release. Since this mod declares an open Sable
 * version range, binding to it would mean a {@code NoSuchMethodError} on a future Sable. The
 * {@code (Level, Position)} overload is the supported one and is behaviourally identical
 * (verified in Sable's bytecode: same containment test, same pose transform, and it returns
 * the very same {@code Vec3} instance when the position is not on a sub-level).</p>
 */
@Mixin(targets = "org.vivecraft.client_vr.gameplay.trackers.TeleportTracker", remap = false)
public abstract class TeleportTrackerSubLevelMixin {

    @WrapOperation(
        method = "activeProcess",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;moveTo(DDD)V"))
    private void vivecraftsable$projectTeleportOutOfSubLevel(
            LocalPlayer player, double x, double y, double z, Operation<Void> original) {

        Vec3 corrected = SubLevelTeleportProjection.correct(
            new Vec3(x, y, z),
            destination -> SableCompanion.INSTANCE.projectOutOfSubLevel(player.level(), (Position) destination));

        original.call(player, corrected.x, corrected.y, corrected.z);
    }
}
