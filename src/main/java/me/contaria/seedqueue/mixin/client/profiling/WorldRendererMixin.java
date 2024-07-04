package me.contaria.seedqueue.mixin.client.profiling;

import me.contaria.seedqueue.SeedQueueProfiler;
import net.minecraft.client.render.WorldRenderer;
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
@Mixin(value = WorldRenderer.class, priority = 500)
public abstract class WorldRendererMixin {

    @Inject(
            method = "setWorld",
            at = @At("HEAD")
    )
    private void profileVanilla_setWorld(CallbackInfo ci) {
        SeedQueueProfiler.push("vanilla");
    }

    @Inject(
            method = "setWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;reload()V"
            )
    )
    private void profileReload(CallbackInfo ci) {
        SeedQueueProfiler.push("reload");
    }

    @Inject(
            method = "setWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Set;clear()V",
                    ordinal = 0
            )
    )
    private void profileClearChunks(CallbackInfo ci) {
        SeedQueueProfiler.push("clear_chunks");
    }

    @Inject(
            method = "setWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/chunk/ChunkBuilder;stop()V"
            )
    )
    private void profileStopBuilder(CallbackInfo ci) {
        SeedQueueProfiler.swap("stop_builder");
    }

    @Inject(
            method = "setWorld",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/render/WorldRenderer;noCullingBlockEntities:Ljava/util/Set;"
            )
    )
    private void profileClearBlockEntities(CallbackInfo ci) {
        SeedQueueProfiler.swap("clear_block_entities");
    }

    @Inject(
            method = "setWorld",
            at = @At("TAIL")
    )
    private void profilePop_setWorld(CallbackInfo ci) {
        SeedQueueProfiler.pop();
        SeedQueueProfiler.pop();
    }

    @Inject(
            method = "reload",
            at = @At("HEAD")
    )
    private void profileTransparencyShader(CallbackInfo ci) {
        SeedQueueProfiler.push("transparency_shader");
    }

    @Inject(
            method = "reload",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/world/ClientWorld;reloadColor()V"
            )
    )
    private void profileReloadColor(CallbackInfo ci) {
        SeedQueueProfiler.swap("reload_color");
    }

    @Inject(
            method = "reload",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/render/WorldRenderer;chunkBuilder:Lnet/minecraft/client/render/chunk/ChunkBuilder;",
                    ordinal = 0
            )
    )
    private void profileChunkBuilder(CallbackInfo ci) {
        SeedQueueProfiler.swap("chunk_builder");
    }

    @Inject(
            method = "reload",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/BuiltChunkStorage;clear()V"
            )
    )
    private void profileClearChunks_reload(CallbackInfo ci) {
        SeedQueueProfiler.swap("clear_chunks");
    }

    @Inject(
            method = "reload",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;clearChunkRenderers()V"
            )
    )
    private void profileClearChunkRenderers_reload(CallbackInfo ci) {
        SeedQueueProfiler.swap("clear_chunk_renderers");
    }

    @Inject(
            method = "reload",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/render/WorldRenderer;noCullingBlockEntities:Ljava/util/Set;",
                    ordinal = 0
            )
    )
    private void profileClearBlockEntities_reload(CallbackInfo ci) {
        SeedQueueProfiler.swap("clear_block_entities");
    }

    @Inject(
            method = "reload",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/client/render/chunk/ChunkBuilder;Lnet/minecraft/world/World;ILnet/minecraft/client/render/WorldRenderer;)Lnet/minecraft/client/render/BuiltChunkStorage;"
            )
    )
    private void profileBuiltChunkStorage(CallbackInfo ci) {
        SeedQueueProfiler.swap("built_chunk_storage");
    }

    @Inject(
            method = "reload",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/BuiltChunkStorage;updateCameraPosition(DD)V"
            )
    )
    private void profileUpdateCameraPos(CallbackInfo ci) {
        SeedQueueProfiler.swap("update_camera_pos");
    }

    @Inject(
            method = "reload",
            at = @At("RETURN")
    )
    private void profilePop_reload(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }
}
