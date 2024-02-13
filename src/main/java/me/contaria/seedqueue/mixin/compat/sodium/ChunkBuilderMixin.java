package me.contaria.seedqueue.mixin.compat.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import me.contaria.seedqueue.SeedQueue;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ChunkBuilder.class, remap = false)
public abstract class ChunkBuilderMixin {

    @ModifyReturnValue(
            method = "getOptimalThreadCount",
            at = @At("RETURN")
    )
    private static int modifyChunkBuilderThreads(int optimalThreadCount) {
        if (SeedQueue.isOnWall() && SeedQueue.config.chunkUpdateThreads > 0) {
            return SeedQueue.config.chunkUpdateThreads;
        }
        return optimalThreadCount;
    }

    // TODO: unsure how helpful this really is, might remove in the future
    @WrapWithCondition(
            method = "stopWorkers",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Thread;join()V"
            )
    )
    private boolean doNotWaitForWorkersToStopOnWall(Thread thread) {
        return !SeedQueue.isOnWall();
    }
}
