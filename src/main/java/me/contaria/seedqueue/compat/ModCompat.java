package me.contaria.seedqueue.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

public class ModCompat {

    public static final boolean HAS_FASTRESET = FabricLoader.getInstance().isModLoaded("fast_reset");
    public static final boolean HAS_SODIUM = FabricLoader.getInstance().isModLoaded("sodium");
    public static final boolean HAS_STANDARDSETTINGS = FabricLoader.getInstance().isModLoaded("standardsettings");
    public static final boolean HAS_WORLDPREVIEW = FabricLoader.getInstance().isModLoaded("worldpreview");

    public static void fastReset$fastReset(MinecraftServer server) {
        if (HAS_FASTRESET) {
            FastResetCompat.fastReset(server);
        }
    }

    public static void sodium$clearShaderCache() {
        if (HAS_SODIUM) {
            SodiumCompat.clearShaderCache();
        }
    }

    public static void sodium$clearBuildBufferPool() {
        if (HAS_SODIUM) {
            SodiumCompat.clearBuildBufferPool();
        }
    }

    public static void standardsettings$cacheAndReset() {
        if (HAS_STANDARDSETTINGS) {
            StandardSettingsCompat.resetPendingActions();
            StandardSettingsCompat.createCache();
            StandardSettingsCompat.reset();
        }
    }

    public static void standardsettings$onWorldJoin() {
        if (HAS_STANDARDSETTINGS) {
            StandardSettingsCompat.onWorldJoin();
        }
    }

    public static boolean worldpreview$isRenderingPreview() {
        if (HAS_WORLDPREVIEW) {
            return WorldPreviewCompat.isRenderingPreview();
        }
        return false;
    }

    public static boolean worldpreview$kill(MinecraftServer server) {
        if (HAS_WORLDPREVIEW) {
            return WorldPreviewCompat.kill(server);
        }
        return false;
    }
}
