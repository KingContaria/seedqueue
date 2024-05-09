package me.contaria.seedqueue.mixin.compat.sodium.profiling;

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.common.util.collections.FutureDequeDrain;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;

/**
 * Profiling mixins add more usage of the profiler to hot paths during wall rendering.
 * Because of the amount of injections this would require, @Overwrites are used where possible instead.
 * These Mixins will be removed in later versions of SeedQueue anyway.
 */
@Mixin(value = ChunkRenderManager.class, remap = false, priority = 500)
public abstract class ChunkRenderManagerMixin<T extends ChunkGraphicsState> {

    @Shadow
    @Final
    private ChunkBuilder<T> builder;

    @Shadow
    @Final
    private ObjectArrayFIFOQueue<ChunkRenderContainer<T>> importantRebuildQueue;

    @Shadow
    public abstract boolean isChunkPrioritized(ChunkRenderContainer<T> render);

    @Shadow
    private boolean dirty;

    @Shadow
    @Final
    private ObjectArrayFIFOQueue<ChunkRenderContainer<T>> rebuildQueue;

    @Shadow
    @Final
    private ChunkRenderBackend<T> backend;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuilder;<init>(Lme/jellysquid/mods/sodium/client/model/vertex/type/ChunkVertexType;Lme/jellysquid/mods/sodium/client/render/chunk/ChunkRenderBackend;)V"))
    private void profileCreateChunkBuilder(CallbackInfo ci) {
        MinecraftClient.getInstance().getProfiler().push("create_chunk_builder");
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuilder;init(Lnet/minecraft/client/world/ClientWorld;Lme/jellysquid/mods/sodium/client/render/chunk/passes/BlockRenderPassManager;)V"))
    private void profileInitChunkBuilder(CallbackInfo ci) {
        MinecraftClient.getInstance().getProfiler().swap("init_chunk_builder");
    }

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lme/jellysquid/mods/sodium/client/render/chunk/ChunkRenderManager;dirty:Z"))
    private void profileCreateRenderLists(CallbackInfo ci) {
        MinecraftClient.getInstance().getProfiler().swap("create_render_lists");
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/cull/graph/ChunkGraphCuller;<init>(Lnet/minecraft/world/World;I)V"))
    private void profileCreateGraphCuller(CallbackInfo ci) {
        MinecraftClient.getInstance().getProfiler().swap("create_graph_culler");
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void profileEnd(CallbackInfo ci) {
        MinecraftClient.getInstance().getProfiler().pop();
    }

    /**
     * @author contaria
     * @reason see JavaDocs on this mixin class
     */
    @Overwrite
    public void updateChunks() {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();
        Deque<CompletableFuture<ChunkBuildResult<T>>> futures = new ArrayDeque<>();

        int budget = this.builder.getSchedulingBudget();
        int submitted = 0;

        profiler.push("important_rebuilds");
        while (!this.importantRebuildQueue.isEmpty()) {
            ChunkRenderContainer<T> render = this.importantRebuildQueue.dequeue();

            // Do not allow distant chunks to block rendering
            if (!this.isChunkPrioritized(render)) {
                this.builder.deferRebuild(render);
            } else {
                futures.add(this.builder.scheduleRebuildTaskAsync(render));
            }

            this.dirty = true;
            submitted++;
        }

        profiler.swap("rebuild");
        while (submitted < budget && !this.rebuildQueue.isEmpty()) {
            ChunkRenderContainer<T> render = this.rebuildQueue.dequeue();

            this.builder.deferRebuild(render);
            submitted++;
        }

        this.dirty |= submitted > 0;

        profiler.swap("pending_uploads");
        // Try to complete some other work on the main thread while we wait for rebuilds to complete
        this.dirty |= this.builder.performPendingUploads();

        if (!futures.isEmpty()) {
            profiler.swap("backend_upload");
            this.backend.upload(RenderDevice.INSTANCE.createCommandList(), new FutureDequeDrain<>(futures));
        }
        profiler.pop();
    }
}
