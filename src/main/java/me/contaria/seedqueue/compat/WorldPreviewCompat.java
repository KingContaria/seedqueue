package me.contaria.seedqueue.compat;

import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.interfaces.WPMinecraftServer;
import net.minecraft.server.MinecraftServer;

class WorldPreviewCompat {

    static boolean kill(MinecraftServer server) {
        return ((WPMinecraftServer) server).worldpreview$kill();
    }

    public static boolean isRenderingPreview() {
        return WorldPreview.renderingPreview;
    }
}
