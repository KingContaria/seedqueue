package me.contaria.seedqueue.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

/**
 * Intermediate class that allows safe access to other mods methods/fields from code paths that may be run without the mod present.
 * This class provides wrapper methods for compat classes which should only be classloaded if the mod in question is loaded.
 */
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

    public static boolean worldpreview$kill(MinecraftServer server) {
        if (HAS_WORLDPREVIEW) {
            return WorldPreviewCompat.kill(server);
        }
        return false;
    }
}
