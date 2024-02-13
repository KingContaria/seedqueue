package me.contaria.seedqueue.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

public class ModCompat {

    public static void fastReset$fastReset(MinecraftServer server) {
        if (FabricLoader.getInstance().isModLoaded("fast_reset")) {
            FastResetCompat.fastReset(server);
        }
    }

    public static boolean worldpreview$kill(MinecraftServer server) {
        if (FabricLoader.getInstance().isModLoaded("worldpreview")) {
            return WorldPreviewCompat.kill(server);
        }
        return false;
    }
}
