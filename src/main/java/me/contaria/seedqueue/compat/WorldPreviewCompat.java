package me.contaria.seedqueue.compat;

import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.interfaces.WPMinecraftServer;
import net.minecraft.server.MinecraftServer;

public class WorldPreviewCompat {

    static boolean inPreview() {
        return WorldPreview.inPreview();
    }

    static boolean kill(MinecraftServer server) {
        return ((WPMinecraftServer) server).worldpreview$kill();
    }
}
