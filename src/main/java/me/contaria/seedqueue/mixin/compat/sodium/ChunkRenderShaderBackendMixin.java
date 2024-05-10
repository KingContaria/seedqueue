package me.contaria.seedqueue.mixin.compat.sodium;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.compat.SodiumCompat;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgram;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkRenderShaderBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ChunkRenderShaderBackend.class, remap = false)
public abstract class ChunkRenderShaderBackendMixin {

    @WrapOperation(
            method = "createShaders",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/shader/ChunkRenderShaderBackend;createShader(Lme/jellysquid/mods/sodium/client/gl/device/RenderDevice;Lme/jellysquid/mods/sodium/client/render/chunk/shader/ChunkFogMode;Lme/jellysquid/mods/sodium/client/gl/attribute/GlVertexFormat;)Lme/jellysquid/mods/sodium/client/render/chunk/shader/ChunkProgram;"
            )
    )
    private ChunkProgram cacheShadersOnWall(ChunkRenderShaderBackend<?> instance, RenderDevice device, ChunkFogMode fogMode, GlVertexFormat<ChunkMeshAttribute> vertexFormat, Operation<ChunkProgram> original) {
        if (SeedQueue.isOnWall() && SeedQueue.config.cacheShaders) {
            return SodiumCompat.WALL_SHADER_CACHE.computeIfAbsent(fogMode, f -> original.call(instance, device, fogMode, vertexFormat));
        }
        return original.call(instance, device, fogMode, vertexFormat);
    }

    @WrapWithCondition(
            method = "delete",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/shader/ChunkProgram;delete()V"
            )
    )
    private boolean doNotDeleteCachedShaders(ChunkProgram program) {
        return !SodiumCompat.WALL_SHADER_CACHE.containsValue(program);
    }
}
