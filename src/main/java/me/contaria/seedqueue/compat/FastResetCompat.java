package me.contaria.seedqueue.compat;

import fast_reset.client.interfaces.FRMinecraftServer;
import net.minecraft.server.MinecraftServer;

class FastResetCompat {

    static void fastReset(MinecraftServer server) {
        ((FRMinecraftServer) server).fastReset$fastReset();
    }
}
