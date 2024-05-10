package me.contaria.seedqueue.compat;

import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.interfaces.WPMinecraftServer;
import net.minecraft.server.MinecraftServer;

public class WorldPreviewCompat {

    public static final ThreadLocal<WorldPreviewProperties> SERVER_WP_PROPERTIES = new ThreadLocal<>();

    static boolean kill(MinecraftServer server) {
        return ((WPMinecraftServer) server).worldpreview$kill();
    }

    static boolean isRenderingPreview() {
        return WorldPreview.renderingPreview;
    }
}
