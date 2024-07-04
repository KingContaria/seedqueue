package me.contaria.seedqueue.mixin.compat.sodium.profiling;

import me.contaria.seedqueue.SeedQueueProfiler;
import me.jellysquid.mods.sodium.client.render.chunk.backends.multidraw.MultidrawChunkRenderBackend;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Profiling mixins add more usage of the profiler to hot paths during wall rendering.
 * These Mixins will be removed in later versions of SeedQueue.
 */
@Debug(export = true)
@Mixin(value = MultidrawChunkRenderBackend.class, remap = false)
public abstract class MultidrawChunkRenderBackendMixin {

    @Inject(
            method = "upload",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/backends/multidraw/MultidrawChunkRenderBackend;setupUploadBatches(Ljava/util/Iterator;)V"
            )
    )
    private void profileSetupUploadBatches(CallbackInfo ci) {
        SeedQueueProfiler.push("setup_upload_batches");
    }

    @Inject(
            method = "upload",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/gl/device/CommandList;bindBuffer(Lme/jellysquid/mods/sodium/client/gl/buffer/GlBufferTarget;Lme/jellysquid/mods/sodium/client/gl/buffer/GlBuffer;)V"
            )
    )
    private void profileBindBuffer(CallbackInfo ci) {
        SeedQueueProfiler.swap("bind_buffer");
    }

    @Inject(
            method = "upload",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/gl/device/CommandList;bindBuffer(Lme/jellysquid/mods/sodium/client/gl/buffer/GlBufferTarget;Lme/jellysquid/mods/sodium/client/gl/buffer/GlBuffer;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void profileIterateQueue(CallbackInfo ci) {
        SeedQueueProfiler.swap("iterate_queue");
    }

    @Inject(
            method = "upload",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayFIFOQueue;dequeue()Ljava/lang/Object;"
            )
    )
    private void profileDequeueUpload(CallbackInfo ci) {
        SeedQueueProfiler.push("dequeue_upload");
    }

    @Inject(
            method = "upload",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/gl/arena/GlBufferArena;prepareBuffer(Lme/jellysquid/mods/sodium/client/gl/device/CommandList;I)V"
            )
    )
    private void profilePrepareBuffer(CallbackInfo ci) {
        SeedQueueProfiler.swap("prepare_buffer");
    }

    @Inject(
            method = "upload",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/gl/arena/GlBufferArena;prepareBuffer(Lme/jellysquid/mods/sodium/client/gl/device/CommandList;I)V",
                    shift = At.Shift.AFTER
            )
    )
    private void profileIterateResults(CallbackInfo ci) {
        SeedQueueProfiler.swap("iterate_results");
    }

    @Inject(
            method = "upload",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/region/ChunkRegion;getTessellation()Lme/jellysquid/mods/sodium/client/gl/tessellation/GlTessellation;",
                    ordinal = 0
            )
    )
    private void profileUpdateTesselation(CallbackInfo ci) {
        SeedQueueProfiler.swap("update_tesselation");
    }

    @Inject(
            method = "upload",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;clear()V"
            )
    )
    private void profileClearQueue(CallbackInfo ci) {
        SeedQueueProfiler.swap("clear_queue");
    }

    @Inject(
            method = "upload",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;clear()V",
                    shift = At.Shift.AFTER
            )
    )
    private void profilePop_iterateQueue(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }

    @Inject(
            method = "upload",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/gl/device/CommandList;invalidateBuffer(Lme/jellysquid/mods/sodium/client/gl/buffer/GlMutableBuffer;)V"
            )
    )
    private void profileInvalidateBuffer(CallbackInfo ci) {
        SeedQueueProfiler.swap("invalidate_buffer");
    }

    @Inject(
            method = "upload",
            at = @At("RETURN")
    )
    private void profilePop_upload(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }

    @Inject(
            method = "setupUploadBatches",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/region/ChunkRegionManager;getRegion(III)Lme/jellysquid/mods/sodium/client/render/chunk/region/ChunkRegion;"
            )
    )
    private void profileGetRegion(CallbackInfo ci) {
        SeedQueueProfiler.push("get_region");
    }

    @Inject(
            method = "setupUploadBatches",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/ChunkRenderContainer;setData(Lme/jellysquid/mods/sodium/client/render/chunk/data/ChunkRenderData;)V"
            )
    )
    private void profilePop_setupUploadBatches_1(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }

    @Inject(
            method = "setupUploadBatches",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/region/ChunkRegionManager;getOrCreateRegion(III)Lme/jellysquid/mods/sodium/client/render/chunk/region/ChunkRegion;"
            )
    )
    private void profileCreateRegion(CallbackInfo ci) {
        SeedQueueProfiler.swap("create_region");
    }

    @Inject(
            method = "setupUploadBatches",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/region/ChunkRegion;getUploadQueue()Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"
            )
    )
    private void profileAddToQueue(CallbackInfo ci) {
        SeedQueueProfiler.swap("add_to_queue");
    }

    @Inject(
            method = "setupUploadBatches",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;add(Ljava/lang/Object;)Z",
                    shift = At.Shift.AFTER
            )
    )
    private void profilePop_setupUploadBatches_2(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }
}
