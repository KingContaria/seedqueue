package me.contaria.seedqueue.compat;

import me.contaria.standardsettings.StandardSettings;
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
    public static final boolean HAS_STATEOUTPUT = FabricLoader.getInstance().isModLoaded("state-output");

    public static void fastReset$fastReset(MinecraftServer server) {
        if (HAS_FASTRESET) {
            FastResetCompat.fastReset(server);
        }
    }

    public static boolean fastReset$shouldSave(MinecraftServer server) {
        if (HAS_FASTRESET) {
            return FastResetCompat.shouldSave(server);
        }
        return false;
    }

    public static void stateoutput$setWallState() {
        if (HAS_STATEOUTPUT) {
            StateOutputCompat.setWallState();
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

    public static void standardsettings$cache() {
        if (HAS_STANDARDSETTINGS) {
            StandardSettingsCompat.createCache();
        }
    }

    public static void standardsettings$reset() {
        if (HAS_STANDARDSETTINGS) {
            StandardSettingsCompat.resetPendingActions();
            if (StandardSettings.isEnabled()) {
                StandardSettingsCompat.reset();
            }
        }
    }

    public static void standardsettings$loadCache() {
        if (HAS_STANDARDSETTINGS) {
            StandardSettingsCompat.loadCache();
        }
    }

    public static boolean worldpreview$inPreview() {
        if (HAS_WORLDPREVIEW) {
            return WorldPreviewCompat.inPreview();
        }
        return false;
    }

    public static boolean worldpreview$kill(MinecraftServer server) {
        if (HAS_WORLDPREVIEW) {
            return WorldPreviewCompat.kill(server);
        }
        return false;
    }

    public static void worldpreview$clearFramebufferPool() {
        if (HAS_WORLDPREVIEW) {
            WorldPreviewFrameBuffer.clearFramebufferPool();
        }
    }
}
