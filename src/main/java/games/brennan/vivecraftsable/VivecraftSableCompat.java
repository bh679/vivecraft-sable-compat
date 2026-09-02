package games.brennan.vivecraftsable;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Vivecraft Sable Compat — makes Vivecraft VR behave on a Sable sub-level.
 *
 * <p>Sable stores a sub-level's blocks in a distant region of the same level and
 * draws them somewhere else entirely, via the sub-level's pose. Any code that
 * reads a position out of the world and then writes it straight back to an
 * entity is therefore working in the wrong frame the moment the player stands on
 * a moving structure. Sable guards the vanilla paths for this itself; Vivecraft's
 * VR code predates Sable and takes its own routes, which the guards do not
 * cover.</p>
 *
 * <p>This mod supplies the missing guards, and nothing else:</p>
 * <ul>
 *   <li>{@link games.brennan.vivecraftsable.mixin.TeleportTrackerSubLevelMixin} —
 *       VR teleport landed the player in the void instead of where they aimed.</li>
 *   <li>{@link games.brennan.vivecraftsable.mixin.SwingTrackerSubLevelAabbMixin} —
 *       VR melee silently hit nothing while standing on a carriage.</li>
 * </ul>
 *
 * <p><b>Client-only, and no Sable internals are touched.</b> Every mixin targets
 * a Vivecraft class; the only Sable code called is the public companion API
 * method {@code SableCompanion.projectOutOfSubLevel}. That is why this mod
 * declares a Sable version <em>range</em> where the sibling Sable mods pin
 * exactly — see the note in {@code gradle.properties}.</p>
 *
 * <p>Nothing here is Dungeon Train specific: these are generic Vivecraft × Sable
 * bugs and the fixes apply to any Sable ship.</p>
 */
@Mod(value = VivecraftSableCompat.MOD_ID, dist = Dist.CLIENT)
public final class VivecraftSableCompat {

    public static final String MOD_ID = "vivecraft_sable_compat";

    private static final Logger LOGGER = LogUtils.getLogger();

    public VivecraftSableCompat(IEventBus modBus, ModContainer modContainer) {
        LOGGER.info("[{}] loaded — Vivecraft VR fixes for Sable sub-levels active", MOD_ID);
    }
}
