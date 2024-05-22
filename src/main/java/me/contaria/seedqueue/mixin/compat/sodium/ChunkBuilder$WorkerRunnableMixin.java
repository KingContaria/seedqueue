package me.contaria.seedqueue.mixin.compat.sodium;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.compat.SodiumCompat;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "me/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuilder$WorkerRunnable", remap = false)
public abstract class ChunkBuilder$WorkerRunnableMixin {

    @Shadow
    @Final
    private ChunkBuildBuffers bufferCache;

    @Inject(
            method = "run",
            at = @At("RETURN")
    )
    private void cacheBuildBuffersOnWall(CallbackInfo ci) {
        if (SeedQueue.isOnWall()) {
            SodiumCompat.WALL_BUILD_BUFFERS_POOL.add(this.bufferCache);
        }
    }
}
