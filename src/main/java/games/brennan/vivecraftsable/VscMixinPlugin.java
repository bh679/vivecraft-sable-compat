package games.brennan.vivecraftsable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Announces, in the log, that each mixin really applied.
 *
 * <p>This exists for one reason: every fix in this mod targets Vivecraft <em>by string name</em>,
 * because Vivecraft is not a compile dependency. A build error therefore cannot tell you that a
 * target moved — only the running game can. And the bug being fixed is reproducible solely in a VR
 * headset on a moving structure, so the person finding out is a tester, not the author.</p>
 *
 * <p>Without this, "teleport still throws me into the void" is ambiguous between <em>the fix is
 * wrong</em> and <em>the fix never loaded</em>. {@link #postApply} is called once per successful
 * application, so the presence or absence of these lines settles that immediately, and costs
 * nothing after load.</p>
 *
 * <p>Deliberately not a gate: unlike DT's old {@code VivecraftMixinPlugin}, this never declines a
 * mixin. Vivecraft is a <em>required</em> dependency here, so if its classes are missing the mod
 * should fail loudly rather than quietly no-op.</p>
 */
public final class VscMixinPlugin implements IMixinConfigPlugin {

    // log4j directly, not slf4j via LogUtils: this runs during early class transformation, before
    // the game's logging facade is a safe thing to touch.
    private static final Logger LOGGER = LogManager.getLogger("vivecraft_sable_compat");

    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.info("{} mixin config loading — a line per applied mixin follows; if one is missing, "
            + "that fix is NOT active", VscDiagnostics.TAG);
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                          IMixinInfo mixinInfo) {
        LOGGER.info("{} mixin applied: {} -> {}", VscDiagnostics.TAG,
            simpleName(mixinClassName), targetClassName);
    }

    private static String simpleName(String className) {
        int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(dot + 1);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                         IMixinInfo mixinInfo) {}
}
