package me.contaria.seedqueue.mixin.compat.sodium.profiling;

import me.contaria.seedqueue.SeedQueueProfiler;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
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
@Mixin(value = ChunkRenderCacheLocal.class, remap = false)
public abstract class ChunkRenderCacheLocalMixin {

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/world/WorldSlice;<init>(Lnet/minecraft/world/World;)V"
            ),
            remap = true
    )
    private void profileWorldSlice(CallbackInfo ci) {
        SeedQueueProfiler.push("world_slice");
    }

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/model/light/cache/ArrayLightDataCache;<init>(Lnet/minecraft/world/BlockRenderView;)V"
            ),
            remap = true
    )
    private void profileLightDataCache(CallbackInfo ci) {
        SeedQueueProfiler.swap("light_data_cache");
    }

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/model/light/LightPipelineProvider;<init>(Lme/jellysquid/mods/sodium/client/model/light/data/LightDataAccess;)V",
                    remap = false
            ),
            remap = true
    )
    private void profileLightPipelineProvider(CallbackInfo ci) {
        SeedQueueProfiler.swap("light_pipeline_provider");
    }

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/pipeline/context/ChunkRenderCacheLocal;createBiomeColorBlender()Lme/jellysquid/mods/sodium/client/model/quad/blender/BiomeColorBlender;",
                    remap = false
            ),
            remap = true
    )
    private void profileBiomeColorBlender(CallbackInfo ci) {
        SeedQueueProfiler.swap("biome_color_blender");
    }

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/pipeline/BlockRenderer;<init>(Lnet/minecraft/client/MinecraftClient;Lme/jellysquid/mods/sodium/client/model/light/LightPipelineProvider;Lme/jellysquid/mods/sodium/client/model/quad/blender/BiomeColorBlender;)V"
            ),
            remap = true
    )
    private void profileBlockRenderer(CallbackInfo ci) {
        SeedQueueProfiler.swap("block_renderer");
    }

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/pipeline/FluidRenderer;<init>(Lnet/minecraft/client/MinecraftClient;Lme/jellysquid/mods/sodium/client/model/light/LightPipelineProvider;Lme/jellysquid/mods/sodium/client/model/quad/blender/BiomeColorBlender;)V"
            ),
            remap = true
    )
    private void profileFluidRenderer(CallbackInfo ci) {
        SeedQueueProfiler.swap("fluid_renderer");
    }

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/model/BakedModelManager;getBlockModels()Lnet/minecraft/client/render/block/BlockModels;"
            ),
            remap = true
    )
    private void profileBlockModels(CallbackInfo ci) {
        SeedQueueProfiler.swap("block_models");
    }

    @Inject(
            method = "<init>",
            at = @At("TAIL"),
            remap = true
    )
    private void profilePop(CallbackInfo ci) {
        SeedQueueProfiler.pop();
    }
}
