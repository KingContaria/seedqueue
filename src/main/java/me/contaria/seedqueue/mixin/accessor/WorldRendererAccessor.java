package me.contaria.seedqueue.mixin.accessor;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldRenderer.class)
public interface WorldRendererAccessor {
    @Accessor("world")
    ClientWorld seedQueue$getWorld();

    @Invoker("getCompletedChunkCount")
    int seedQueue$getCompletedChunkCount();

    @Invoker("scheduleChunkRender")
    void seedQueue$scheduleChunkRender(int x, int y, int z, boolean important);
}
