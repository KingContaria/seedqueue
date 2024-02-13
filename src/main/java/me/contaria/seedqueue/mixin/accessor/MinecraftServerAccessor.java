package me.contaria.seedqueue.mixin.accessor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.UserCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
    @Accessor("running")
    void seedQueue$setRunning(boolean running);

    @Mutable
    @Accessor("userCache")
    void seedQueue$setUserCache(UserCache userCache);
}
