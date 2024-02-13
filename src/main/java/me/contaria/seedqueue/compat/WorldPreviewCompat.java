package me.contaria.seedqueue.compat;

import me.voidxwalker.worldpreview.interfaces.WPMinecraftServer;
import net.minecraft.server.MinecraftServer;

class WorldPreviewCompat {

    static boolean kill(MinecraftServer server) {
        return ((WPMinecraftServer) server).worldpreview$kill();
    }
}
