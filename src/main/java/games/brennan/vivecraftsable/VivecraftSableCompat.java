package games.brennan.vivecraftsable;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

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

    public VivecraftSableCompat(IEventBus modBus, ModContainer modContainer) {
        // Diagnostics exist because this mod's bug is only reproducible in a VR headset on a moving
        // structure, which the author cannot run — a tester's latest.log has to be enough on its
        // own. See VscDiagnostics.
        modBus.addListener(FMLClientSetupEvent.class, e -> VscDiagnostics.logEnvironment());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class,
            e -> VscDiagnostics.logSummary());
    }
}
