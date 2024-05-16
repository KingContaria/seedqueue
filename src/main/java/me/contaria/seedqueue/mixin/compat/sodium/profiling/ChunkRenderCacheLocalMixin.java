package me.contaria.seedqueue.mixin.compat.sodium.profiling;

import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
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
@Mixin(value = ChunkRenderCacheLocal.class, remap = false)
public abstract class ChunkRenderCacheLocalMixin {

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/world/WorldSlice;<init>(Lnet/minecraft/world/World;)V", remap = true))
    private void profileWorldSlice(CallbackInfo ci) {
        if (MinecraftClient.getInstance().isOnThread()) MinecraftClient.getInstance().getProfiler().push("world_slice");
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/model/light/cache/ArrayLightDataCache;<init>(Lnet/minecraft/world/BlockRenderView;)V", remap = true))
    private void profileLightDataCache(CallbackInfo ci) {
        if (MinecraftClient.getInstance().isOnThread()) MinecraftClient.getInstance().getProfiler().swap("light_data_cache");
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/model/light/LightPipelineProvider;<init>(Lme/jellysquid/mods/sodium/client/model/light/data/LightDataAccess;)V"))
    private void profileLightPipelineProvider(CallbackInfo ci) {
        if (MinecraftClient.getInstance().isOnThread()) MinecraftClient.getInstance().getProfiler().swap("light_pipeline_provider");
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/pipeline/context/ChunkRenderCacheLocal;createBiomeColorBlender()Lme/jellysquid/mods/sodium/client/model/quad/blender/BiomeColorBlender;", remap = true))
    private void profileBiomeColorBlender(CallbackInfo ci) {
        if (MinecraftClient.getInstance().isOnThread()) MinecraftClient.getInstance().getProfiler().swap("biome_color_blender");
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/pipeline/BlockRenderer;<init>(Lnet/minecraft/client/MinecraftClient;Lme/jellysquid/mods/sodium/client/model/light/LightPipelineProvider;Lme/jellysquid/mods/sodium/client/model/quad/blender/BiomeColorBlender;)V"))
    private void profileBlockRenderer(CallbackInfo ci) {
        if (MinecraftClient.getInstance().isOnThread()) MinecraftClient.getInstance().getProfiler().swap("block_renderer");
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/pipeline/FluidRenderer;<init>(Lnet/minecraft/client/MinecraftClient;Lme/jellysquid/mods/sodium/client/model/light/LightPipelineProvider;Lme/jellysquid/mods/sodium/client/model/quad/blender/BiomeColorBlender;)V"))
    private void profileFluidRenderer(CallbackInfo ci) {
        if (MinecraftClient.getInstance().isOnThread()) MinecraftClient.getInstance().getProfiler().swap("fluid_renderer");
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/model/BakedModelManager;getBlockModels()Lnet/minecraft/client/render/block/BlockModels;", remap = true))
    private void profileBlockModels(CallbackInfo ci) {
        if (MinecraftClient.getInstance().isOnThread()) MinecraftClient.getInstance().getProfiler().swap("block_models");
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void profileEnd(CallbackInfo ci) {
        if (MinecraftClient.getInstance().isOnThread()) MinecraftClient.getInstance().getProfiler().pop();
    }
}
