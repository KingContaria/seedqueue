package me.contaria.seedqueue.mixin.accessor;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
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

    @Mutable
    @Accessor("gameProfileRepo")
    void seedQueue$setGameProfileRepo(GameProfileRepository gameProfileRepo);

    @Mutable
    @Accessor("sessionService")
    void seedQueue$setSessionService(MinecraftSessionService sessionService);
}
