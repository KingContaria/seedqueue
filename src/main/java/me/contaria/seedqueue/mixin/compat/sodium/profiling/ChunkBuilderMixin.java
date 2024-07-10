package me.contaria.seedqueue.mixin.compat.sodium.profiling;

import me.contaria.seedqueue.SeedQueueProfiler;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Profiling mixins add more usage of the profiler to hot paths during wall rendering.
 * These Mixins will be removed in later versions of SeedQueue.
 */
@Mixin(value = ChunkBuilder.class, remap = false)
public abstract class ChunkBuilderMixin {

    @Inject(
            method = "startWorkers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;getInstance()Lnet/minecraft/client/MinecraftClient;",
                    remap = true
            )
    )
    private void profileStartWorkers(CallbackInfo ci) {
        SeedQueueProfiler.push("start_workers");
    }

    @Inject(
            method = "startWorkers",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;<init>(Lme/jellysquid/mods/sodium/client/model/vertex/type/ChunkVertexType;Lme/jellysquid/mods/sodium/client/render/chunk/passes/BlockRenderPassManager;)V"
            )
    )
    private void profileBuildBuffers(CallbackInfo ci) {
        SeedQueueProfiler.push("build_buffers");
    }

    @Inject(
            method = "startWorkers",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/pipeline/context/ChunkRenderCacheLocal;<init>(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/world/World;)V",
                    remap = true
            )
    )
    private void profileRenderCache(CallbackInfo ci) {
        SeedQueueProfiler.swap("render_cache");
    }

    @Inject(
            method = "startWorkers",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuilder$WorkerRunnable;<init>(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuilder;Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;Lme/jellysquid/mods/sodium/client/render/pipeline/context/ChunkRenderCacheLocal;)V"
            )
    )
    private void profileWorker(CallbackInfo ci) {
        SeedQueueProfiler.swap("worker");
    }

    @Inject(
            method = "startWorkers",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Thread;<init>(Ljava/lang/Runnable;Ljava/lang/String;)V"
            )
    )
    private void profileThread(CallbackInfo ci) {
        SeedQueueProfiler.swap("thread");
    }

    @Inject(
            method = "startWorkers",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z"
            )
    )
    private void profilePopPerWorker(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }

    @Inject(
            method = "startWorkers",
            at = @At("TAIL")
    )
    private void profilePop(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }
}
