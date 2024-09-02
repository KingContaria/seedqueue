package me.contaria.seedqueue.mixin.compat.sodium.profiling;

import me.contaria.seedqueue.debug.SeedQueueProfiler;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Profiling mixins add more usage of the profiler to hot paths during wall rendering.
 * These Mixins will be removed in later versions of SeedQueue.
 */
@Mixin(value = SodiumWorldRenderer.class, remap = false, priority = 500)
public abstract class SodiumWorldRendererMixin implements ChunkStatusListener {

    @Inject(
            method = "setWorld",
            at = @At(
                    value = "FIELD",
                    target = "Lme/jellysquid/mods/sodium/client/render/SodiumWorldRenderer;world:Lnet/minecraft/client/world/ClientWorld;",
                    ordinal = 1
            ),
            remap = true
    )
    private void profileSodium_setWorld(CallbackInfo ci) {
        SeedQueueProfiler.push("sodium");
    }

    @Inject(
            method = "setWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/SodiumWorldRenderer;unloadWorld()V",
                    remap = false
            ),
            remap = true
    )
    private void profileUnloadWorld(CallbackInfo ci) {
        SeedQueueProfiler.push("unload_world");
    }

    @Inject(
            method = "setWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/SodiumWorldRenderer;unloadWorld()V",
                    shift = At.Shift.AFTER,
                    remap = false
            ),
            remap = true
    )
    private void profilePopUnloadWorld(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }

    @Inject(
            method = "setWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/SodiumWorldRenderer;loadWorld(Lnet/minecraft/client/world/ClientWorld;)V"
            ),
            remap = true
    )
    private void profileLoadWorld(CallbackInfo ci) {
        SeedQueueProfiler.push("load_world");
    }

    @Inject(
            method = "setWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/SodiumWorldRenderer;loadWorld(Lnet/minecraft/client/world/ClientWorld;)V",
                    shift = At.Shift.AFTER
            ),
            remap = true
    )
    private void profilePopLoadWorld(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }

    @Inject(
            method = "setWorld",
            at = @At("TAIL"),
            remap = true
    )
    private void profilePop_setWorld(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }

    @Inject(
            method = "loadWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/pipeline/context/ChunkRenderCacheShared;createRenderContext(Lnet/minecraft/world/BlockRenderView;)V"
            ),
            remap = true
    )
    private void profileCreateRenderContext(CallbackInfo ci) {
        SeedQueueProfiler.push("create_render_context");
    }

    @Inject(
            method = "loadWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/SodiumWorldRenderer;initRenderer()V",
                    remap = false
            ),
            remap = true
    )
    private void profileInitRenderer(CallbackInfo ci) {
        SeedQueueProfiler.swap("init_renderer");
    }

    @Inject(
            method = "loadWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/world/ChunkStatusListenerManager;setListener(Lme/jellysquid/mods/sodium/client/world/ChunkStatusListener;)V",
                    remap = false
            ),
            remap = true
    )
    private void profileSetListener(CallbackInfo ci) {
        SeedQueueProfiler.swap("set_listener");
    }

    @Inject(
            method = "loadWorld",
            at = @At("TAIL"),
            remap = true
    )
    private void profilePop_loadWorld(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }

    @Inject(
            method = "unloadWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/pipeline/context/ChunkRenderCacheShared;destroyRenderContext(Lnet/minecraft/world/BlockRenderView;)V",
                    remap = true
            )
    )
    private void profileDestroyRenderContext(CallbackInfo ci) {
        SeedQueueProfiler.push("destroy_render_context");
    }

    @Inject(
            method = "unloadWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/ChunkRenderManager;destroy()V"
            )
    )
    private void profileDestroyRenderManager(CallbackInfo ci) {
        SeedQueueProfiler.swap("destroy_render_manager");
    }

    @Inject(
            method = "unloadWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/ChunkRenderBackend;delete()V"
            )
    )
    private void profileDeleteRenderBackend(CallbackInfo ci) {
        SeedQueueProfiler.swap("delete_render_backend");
    }

    @Inject(
            method = "unloadWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/longs/LongSet;clear()V"
            )
    )
    private void profileClear(CallbackInfo ci) {
        SeedQueueProfiler.swap("clear");
    }

    @Inject(
            method = "unloadWorld",
            at = @At("TAIL")
    )
    private void profilePop_unloadWorld(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }

    @Inject(
            method = "reload",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/SodiumWorldRenderer;initRenderer()V"
            )
    )
    private void profileSodiumReload(CallbackInfo ci) {
        SeedQueueProfiler.push("sodium");
    }

    @Inject(
            method = "reload",
            at = @At("TAIL")
    )
    private void profilePop_reload(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }

    @Inject(
            method = "initRenderer",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/ChunkRenderManager;destroy()V"
            )
    )
    private void profileDestroyRenderManager_initRenderer(CallbackInfo ci) {
        SeedQueueProfiler.push("destroy_render_manager");
    }

    @Inject(
            method = "initRenderer",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/ChunkRenderManager;destroy()V",
                    shift = At.Shift.AFTER
            )
    )
    private void profilePopDestroyRenderManager_initRenderer(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }

    @Inject(
            method = "initRenderer",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/ChunkRenderBackend;delete()V"
            )
    )
    private void profileDeleteRenderBackend_initRenderer(CallbackInfo ci) {
        SeedQueueProfiler.push("delete_render_backend");
    }

    @Inject(
            method = "initRenderer",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/ChunkRenderBackend;delete()V",
                    shift = At.Shift.AFTER
            )
    )
    private void profilePopDeleteRenderBackend_initRenderer(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }

    @Inject(
            method = "initRenderer",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/passes/BlockRenderPassManager;createDefaultMappings()Lme/jellysquid/mods/sodium/client/render/chunk/passes/BlockRenderPassManager;"
            )
    )
    private void profileCreateDefaultMappings(CallbackInfo ci) {
        SeedQueueProfiler.push("create_default_mappings");
    }

    @Inject(
            method = "initRenderer",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/SodiumWorldRenderer;createChunkRenderBackend(Lme/jellysquid/mods/sodium/client/gl/device/RenderDevice;Lme/jellysquid/mods/sodium/client/gui/SodiumGameOptions;Lme/jellysquid/mods/sodium/client/model/vertex/type/ChunkVertexType;)Lme/jellysquid/mods/sodium/client/render/chunk/ChunkRenderBackend;"
            )
    )
    private void profileCreateRenderBackend(CallbackInfo ci) {
        SeedQueueProfiler.swap("create_render_backend");
    }

    @Inject(
            method = "initRenderer",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/ChunkRenderBackend;createShaders(Lme/jellysquid/mods/sodium/client/gl/device/RenderDevice;)V"
            )
    )
    private void profileCreateShaders(CallbackInfo ci) {
        SeedQueueProfiler.swap("create_shaders");
    }

    @Inject(
            method = "initRenderer",
            at = @At(
                    value = "NEW",
                    target = "(Lme/jellysquid/mods/sodium/client/render/SodiumWorldRenderer;Lme/jellysquid/mods/sodium/client/render/chunk/ChunkRenderBackend;Lme/jellysquid/mods/sodium/client/render/chunk/passes/BlockRenderPassManager;Lnet/minecraft/client/world/ClientWorld;I)Lme/jellysquid/mods/sodium/client/render/chunk/ChunkRenderManager;",
                    remap = true
            )
    )
    private void profileCreateRenderManager(CallbackInfo ci) {
        SeedQueueProfiler.swap("create_render_manager");
    }

    @Inject(
            method = "initRenderer",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/ChunkRenderManager;restoreChunks(Lit/unimi/dsi/fastutil/longs/LongCollection;)V"
            )
    )
    private void profileRestoreChunks(CallbackInfo ci) {
        SeedQueueProfiler.swap("restore_chunks");
    }

    @Inject(
            method = "initRenderer",
            at = @At("TAIL")
    )
    private void profilePop_initRenderer(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }
}
