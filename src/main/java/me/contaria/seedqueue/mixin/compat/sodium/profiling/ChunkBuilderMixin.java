package me.contaria.seedqueue.mixin.compat.sodium.profiling;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Profiling mixins add more usage of the profiler to hot paths during wall rendering.
 * Because of the amount of injections this would require, @Overwrites are used where possible instead.
 * These Mixins will be removed in later versions of SeedQueue anyway.
 */
@Mixin(value = ChunkBuilder.class, remap = false)
public abstract class ChunkBuilderMixin {

    @Inject(method = "startWorkers", at = @At("HEAD"))
    private void profileStartWorkers(CallbackInfo ci) {
        MinecraftClient.getInstance().getProfiler().push("start_workers");
    }

    @Inject(method = "startWorkers", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;<init>(Lme/jellysquid/mods/sodium/client/model/vertex/type/ChunkVertexType;Lme/jellysquid/mods/sodium/client/render/chunk/passes/BlockRenderPassManager;)V"))
    private void profileBuildBuffers(CallbackInfo ci) {
        MinecraftClient.getInstance().getProfiler().push("build_buffers");
    }

    @Inject(method = "startWorkers", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/pipeline/context/ChunkRenderCacheLocal;<init>(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/world/World;)V"))
    private void profileRenderCache(CallbackInfo ci) {
        MinecraftClient.getInstance().getProfiler().swap("render_cache");
    }

    @Inject(method = "startWorkers", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuilder$WorkerRunnable;<init>(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuilder;Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;Lme/jellysquid/mods/sodium/client/render/pipeline/context/ChunkRenderCacheLocal;)V"))
    private void profileWorker(CallbackInfo ci) {
        MinecraftClient.getInstance().getProfiler().swap("worker");
    }

    @Inject(method = "startWorkers", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;<init>(Ljava/lang/Runnable;Ljava/lang/String;)V"))
    private void profileThread(CallbackInfo ci) {
        MinecraftClient.getInstance().getProfiler().swap("thread");
    }

    @Inject(method = "startWorkers", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private void profilePop(CallbackInfo ci) {
        MinecraftClient.getInstance().getProfiler().pop();
    }

    @Inject(method = "startWorkers", at = @At("TAIL"))
    private void profileEnd(CallbackInfo ci) {
        MinecraftClient.getInstance().getProfiler().pop();
    }
}
