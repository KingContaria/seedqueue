package me.contaria.seedqueue.mixin.compat.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.compat.SodiumCompat;
import me.contaria.seedqueue.interfaces.sodium.SQChunkBuilder$WorkerRunnable;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(value = ChunkBuilder.class, remap = false)
public abstract class ChunkBuilderMixin {

    @Shadow
    private World world;

    @Mutable
    @Shadow
    @Final
    private AtomicBoolean running;

    @ModifyReturnValue(
            method = "getMaxThreadCount",
            at = @At("RETURN")
    )
    private static int modifyMaxThreads(int maxThreads) {
        if (SeedQueue.isOnWall()) {
            return SeedQueue.config.getChunkUpdateThreads();
        }
        return maxThreads;
    }

    @ModifyArg(
            method = "createWorker",
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

    // because ChunkBuilderMixin#doNotWaitForWorkersToStopOnWall prevents waiting for old workers to shut down,
    // it's necessary to replace the atomic boolean to prevent the worker threads staying alive
    // when new workers are started before old ones have stopped
    @Inject(
            method = "stopWorkers",
            at = @At("TAIL")
    )
    private void replaceRunningAtomicBooleanOnWall(CallbackInfo ci) {
        if (SeedQueue.isOnWall()) {
            this.running = new AtomicBoolean();
        }
    }

    // mac sodium compat is very silly
    @Group(name = "loadCachedBuildBuffersOnWall")
    @WrapOperation(
            method = "createWorker",
            at = @At(
                    value = "NEW",
                    target = "(Lme/jellysquid/mods/sodium/client/model/vertex/type/ChunkVertexType;Lme/jellysquid/mods/sodium/client/render/chunk/passes/BlockRenderPassManager;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;"
            )
    )
    private ChunkBuildBuffers loadCachedBuildBuffersOnWall(@Coerce Object passId, BlockRenderPassManager buffers, Operation<ChunkBuildBuffers> original) {
        if (SeedQueue.isOnWall() && !SodiumCompat.WALL_BUILD_BUFFERS_POOL.isEmpty()) {
            return Objects.requireNonNull(SodiumCompat.WALL_BUILD_BUFFERS_POOL.remove(0));
        }
        return original.call(passId, buffers);
    }

    @Dynamic
    @Group(name = "loadCachedBuildBuffersOnWall", min = 1, max = 1)
    @WrapOperation(
            method = "createWorker",
            at = @At(
                    value = "NEW",
                    target = "(Lme/jellysquid/mods/sodium/client/gl/attribute/GlVertexFormat;Lme/jellysquid/mods/sodium/client/render/chunk/passes/BlockRenderPassManager;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;"
            )
    )
    private ChunkBuildBuffers loadCachedBuildBuffersOnWall_macSodium(@Coerce Object format, BlockRenderPassManager buffers, Operation<ChunkBuildBuffers> original) {
        if (SeedQueue.isOnWall() && !SodiumCompat.WALL_BUILD_BUFFERS_POOL.isEmpty()) {
            return Objects.requireNonNull(SodiumCompat.WALL_BUILD_BUFFERS_POOL.remove(0));
        }
        return original.call(format, buffers);
    }

    @WrapOperation(
            method = "createWorker",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/world/World;)Lme/jellysquid/mods/sodium/client/render/pipeline/context/ChunkRenderCacheLocal;",
                    remap = true
            ),
            require = 0
    )
    private ChunkRenderCacheLocal createRenderCacheOnWorkerThread(MinecraftClient client, World world, Operation<ChunkRenderCacheLocal> original) {
        if (SeedQueue.isOnWall()) {
            return null;
        }
        return original.call(client, world);
    }

    @SuppressWarnings("InvalidInjectorMethodSignature") // MCDev doesn't seem to like @Coerce on the return type
    @ModifyExpressionValue(
            method = "createWorker",
            at = @At(
                    value = "NEW",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuilder$WorkerRunnable;"
            )
    )
    private @Coerce Object passWorldToWorkerThread(@Coerce Object worker) {
        if (worker instanceof SQChunkBuilder$WorkerRunnable) {
            ((SQChunkBuilder$WorkerRunnable) worker).seedQueue$setWorldForRenderCache(this.world);
        }
        return worker;
    }

    @WrapWithCondition(
            method = "startWorkers",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V"
            )
    )
    private boolean suppressStartWorkersLogOnWall(Logger logger, String s, Object o) {
        return !SeedQueue.isOnWall();
    }

    @WrapWithCondition(
            method = "stopWorkers",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;)V"
            )
    )
    private boolean suppressStopWorkersLogOnWall(Logger logger, String s) {
        return !SeedQueue.isOnWall();
    }

    @ModifyExpressionValue(
            method = "getSchedulingBudget",
            at = @At(
                    value = "CONSTANT",
                    args = "intValue=2")
    )
    private int reduceSchedulingBudgetOnWall(int TASK_QUEUE_LIMIT_PER_WORKER) {
        if (SeedQueue.isOnWall()) {
            return 1;
        }
        return TASK_QUEUE_LIMIT_PER_WORKER;
    }
}
