package me.contaria.seedqueue.mixin.compat.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.compat.SodiumCompat;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Objects;

@Mixin(value = ChunkBuilder.class, remap = false)
public abstract class ChunkBuilderMixin {

    @ModifyReturnValue(
            method = "getOptimalThreadCount",
            at = @At("RETURN")
    )
    private static int modifyChunkUpdateThreads(int optimalThreadCount) {
        if (SeedQueue.isOnWall() && SeedQueue.config.chunkUpdateThreads > 0) {
            return SeedQueue.config.chunkUpdateThreads;
        }
        return optimalThreadCount;
    }

    @ModifyArg(
            method = "startWorkers",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Thread;setPriority(I)V"
            )
    )
    private int modifyChunkUpdateThreadPriority(int priority) {
        if (SeedQueue.isOnWall()) {
            return SeedQueue.config.chunkUpdateThreadPriority;
        }
        return priority;
    }

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

    @WrapOperation(
            method = "startWorkers",
            at = @At(
                    value = "NEW",
                    target = "(Lme/jellysquid/mods/sodium/client/model/vertex/type/ChunkVertexType;Lme/jellysquid/mods/sodium/client/render/chunk/passes/BlockRenderPassManager;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;")
    )
    private ChunkBuildBuffers loadCachedBuildBuffersOnWall(ChunkVertexType passId, BlockRenderPassManager buffers, Operation<ChunkBuildBuffers> original) {
        if (SeedQueue.isOnWall() && !SodiumCompat.WALL_BUILD_BUFFERS_POOL.isEmpty()) {
            return Objects.requireNonNull(SodiumCompat.WALL_BUILD_BUFFERS_POOL.remove(0));
        }
        return original.call(passId, buffers);
    }
}
