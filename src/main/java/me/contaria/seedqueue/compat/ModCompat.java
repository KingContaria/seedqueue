package me.contaria.seedqueue.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

public class ModCompat {

    public static final boolean HAS_FASTRESET = FabricLoader.getInstance().isModLoaded("fast_reset");
    public static final boolean HAS_WORLDPREVIEW = FabricLoader.getInstance().isModLoaded("worldpreview");

    public static void fastReset$fastReset(MinecraftServer server) {
        if (HAS_FASTRESET) {
            FastResetCompat.fastReset(server);
        }
    }

    public static boolean worldpreview$kill(MinecraftServer server) {
        if (HAS_WORLDPREVIEW) {
            return WorldPreviewCompat.kill(server);
        }
        return false;
    }
}
