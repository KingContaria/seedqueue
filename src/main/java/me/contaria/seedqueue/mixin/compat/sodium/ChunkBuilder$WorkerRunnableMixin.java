package me.contaria.seedqueue.mixin.compat.sodium;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.compat.SodiumCompat;
import me.contaria.seedqueue.interfaces.sodium.SQChunkBuilder$WorkerRunnable;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "me/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuilder$WorkerRunnable", remap = false)
public abstract class ChunkBuilder$WorkerRunnableMixin implements SQChunkBuilder$WorkerRunnable {

    @Shadow
    @Final
    private ChunkBuildBuffers bufferCache;

    @Mutable
    @Shadow
    @Final
    private ChunkRenderCacheLocal cache;

    @Unique
    private World world;

    @Inject(
            method = "run",
            at = @At("RETURN")
    )
    private void cacheBuildBuffersOnWall(CallbackInfo ci) {
        if (SeedQueue.config.cacheBuildBuffers) {
            SodiumCompat.WALL_BUILD_BUFFERS_POOL.add(this.bufferCache);
        }
    }

    @Inject(
            method = "run",
            at = @At("HEAD")
    )
    private void createRenderCacheOnWorkerThread(CallbackInfo ci) {
        if (this.cache == null) {
            this.cache = new ChunkRenderCacheLocal(MinecraftClient.getInstance(), this.world);
        }
        this.world = null;
    }

    @Override
    public void seedQueue$setWorldForRenderCache(World world) {
        if (this.cache == null) {
            this.world = world;
        }
    }
}
