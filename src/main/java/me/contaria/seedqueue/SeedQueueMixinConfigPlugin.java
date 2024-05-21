package me.contaria.seedqueue;

import net.fabricmc.loader.api.FabricLoader;
import org.mcsr.speedrunapi.mixin_plugin.SpeedrunMixinConfigPlugin;

public class SeedQueueMixinConfigPlugin extends SpeedrunMixinConfigPlugin {

    private static final boolean MAC_SODIUM = FabricLoader.getInstance().isModLoaded("sodiummac");
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Sodium Mac "Compat"
        // Convert Data On Server breaks with this, but it's broken anyway so whatever
        // Create RenderCache Async doesn't apply on Sodium Mac
        if (MAC_SODIUM) {
            if (mixinClassName.startsWith(this.mixinPackage + ".compat.sodium.profiling")) {
                return false;
            }
            if (mixinClassName.equals(this.mixinPackage + ".compat.sodium.ClientChunkManagerMixin") || mixinClassName.equals(this.mixinPackage + ".compat.sodium.ChunkBuilder$WorkerRunnableMixin2")) {
                return false;
            }
        }
        return super.shouldApplyMixin(targetClassName, mixinClassName);
    }
}
